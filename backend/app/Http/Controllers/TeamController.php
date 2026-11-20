<?php

namespace App\Http\Controllers;

use App\Services\RbacService;
use Illuminate\Http\JsonResponse;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Facades\Schema;
use Illuminate\Support\Str;

/**
 * In-App Business Team & Custom Role Management API.
 * Allows Business Owners (and Super Admins) to manage Co-Owners, Salesmen, Field Officers,
 * Moderators, and custom feature-permission roles strictly scoped to their own business.
 */
class TeamController extends Controller
{
    public function __construct(private readonly RbacService $rbac)
    {
    }

    private function authorizeOwner(Request $request, string $businessId): array|JsonResponse
    {
        $auth = $this->rbac->authenticateApiRequest($request);
        if (!$auth || !$auth['user']) {
            return response()->json(['ok' => false, 'message' => 'Unauthorized: please sign in.'], 401);
        }

        $user = $auth['user'];
        $canManage = $this->rbac->hasPermission($user, 'manage_team')
            || $this->rbac->hasPermission($user, 'manage_roles');
        if (!$canManage) {
            return response()->json(['ok' => false, 'message' => 'Forbidden: only Business Owners can manage team & roles.'], 403);
        }

        if ($businessId === '' || !$this->rbac->canAccessBusiness($user, $businessId)) {
            return response()->json(['ok' => false, 'message' => 'Forbidden: you do not have access to this business.'], 403);
        }

        $business = DB::table('businesses')->where('id', $businessId)->where('is_deleted', false)->first();
        if (!$business) {
            return response()->json(['ok' => false, 'message' => 'Business not found.'], 404);
        }

        return ['user' => $user, 'business' => $business];
    }

    private function allowedPermissionKeysForCaller(array $caller): array
    {
        $catalog = config('sync.permissions', []);
        $isSuperAdmin = $this->rbac->hasPermission($caller, 'manage_roles');
        if ($isSuperAdmin) {
            return $catalog;
        }
        // Business Owners cannot grant global Super Admin web panel permission ('manage_roles')
        unset($catalog['manage_roles']);
        return $catalog;
    }

    public function overview(Request $request): JsonResponse
    {
        $businessId = trim((string) $request->query('businessId', ''));
        $ctx = $this->authorizeOwner($request, $businessId);
        if ($ctx instanceof JsonResponse) {
            return $ctx;
        }

        $caller = $ctx['user'];
        $business = $ctx['business'];
        $hasRoleBizCol = Schema::hasColumn('roles', 'business_id');
        $isSuperAdmin = $this->rbac->hasPermission($caller, 'manage_roles');

        $rolesQuery = DB::table('roles')->orderByDesc('is_system')->orderBy('name');
        if ($hasRoleBizCol) {
            $rolesQuery->where(function ($q) use ($businessId) {
                $q->whereNull('business_id')
                    ->orWhere('business_id', '')
                    ->orWhere('business_id', $businessId);
            });
        }

        $rawRoles = $rolesQuery->get()->filter(function ($r) use ($isSuperAdmin) {
            return $isSuperAdmin || !$r->is_system;
        });

        $roleMap = $rawRoles->keyBy('id');

        $allUsers = DB::table('app_users')->orderBy('created_at_ms')->get();
        $scopedUsers = $allUsers->filter(function ($u) use ($businessId, $business) {
            if ($u->id === 'user-super-admin') {
                return false;
            }
            $bizIds = json_decode((string) $u->business_ids_json, true) ?: [];
            if (in_array($businessId, array_map('strval', $bizIds), true)) {
                return true;
            }
            return !empty($business->owner_user_id) && $business->owner_user_id === $u->id;
        })->values();

        $userCountByRole = $scopedUsers->groupBy('role_id')->map->count();

        $roles = $rawRoles->map(function ($r) use ($userCountByRole, $hasRoleBizCol) {
            $bizCol = $hasRoleBizCol ? (string) ($r->business_id ?? '') : '';
            return [
                'id' => (string) $r->id,
                'businessId' => $bizCol,
                'name' => (string) $r->name,
                'description' => (string) ($r->description ?? ''),
                'permissions' => array_values(json_decode((string) $r->permissions_json, true) ?: []),
                'isSystem' => (bool) $r->is_system || $bizCol === '',
                'isEditable' => !$r->is_system && $bizCol !== '',
                'userCount' => (int) ($userCountByRole[$r->id] ?? 0),
            ];
        })->values();

        $users = $scopedUsers->map(function ($u) use ($roleMap, $business) {
            $role = $roleMap[$u->role_id] ?? DB::table('roles')->where('id', $u->role_id)->first();
            $perms = $role ? (json_decode((string) $role->permissions_json, true) ?: []) : [];
            $isPrimaryOwner = (!empty($business->owner_user_id) && $business->owner_user_id === $u->id);
            $isOwnerRole = $u->role_id === 'role-business-owner' || in_array('manage_team', $perms, true);
            return [
                'id' => (string) $u->id,
                'name' => (string) $u->name,
                'email' => (string) $u->email,
                'roleId' => (string) $u->role_id,
                'roleName' => (string) ($role->name ?? 'Staff'),
                'permissions' => array_values($perms),
                'isActive' => (bool) $u->is_active,
                'isOwner' => $isPrimaryOwner || $isOwnerRole,
                'isPrimaryOwner' => $isPrimaryOwner,
                'lastLoginMs' => (int) $u->last_login_ms,
            ];
        })->values();

        $catalogMap = $this->allowedPermissionKeysForCaller($caller);
        $permissionsCatalog = [];
        foreach ($catalogMap as $key => $label) {
            $permissionsCatalog[] = ['key' => (string) $key, 'label' => (string) $label];
        }

        return response()->json([
            'ok' => true,
            'businessId' => $businessId,
            'businessName' => (string) $business->name,
            'roles' => $roles,
            'users' => $users,
            'permissionsCatalog' => $permissionsCatalog,
        ]);
    }

    public function storeRole(Request $request): JsonResponse
    {
        $businessId = trim((string) $request->input('businessId', ''));
        $ctx = $this->authorizeOwner($request, $businessId);
        if ($ctx instanceof JsonResponse) {
            return $ctx;
        }

        $name = trim((string) $request->input('name', ''));
        $description = trim((string) $request->input('description', ''));
        $requestedPerms = $request->input('permissions', []);

        if ($name === '') {
            return response()->json(['ok' => false, 'message' => 'Role name is required.'], 422);
        }

        $allowedKeys = array_keys($this->allowedPermissionKeysForCaller($ctx['user']));
        $chosen = array_values(array_intersect($allowedKeys, is_array($requestedPerms) ? $requestedPerms : []));
        if (empty($chosen)) {
            $chosen = ['view_dashboard'];
        }

        $hasRoleBizCol = Schema::hasColumn('roles', 'business_id');
        $existsQuery = DB::table('roles')->where('name', $name);
        if ($hasRoleBizCol) {
            $existsQuery->where(function ($q) use ($businessId) {
                $q->where('business_id', $businessId)->orWhereNull('business_id');
            });
        }
        if ($existsQuery->exists()) {
            return response()->json(['ok' => false, 'message' => 'A role with this name already exists.'], 422);
        }

        $now = $this->rbac->nowMs();
        $id = (string) Str::uuid();
        $payload = [
            'id' => $id,
            'name' => substr($name, 0, 100),
            'description' => substr($description, 0, 255),
            'permissions_json' => json_encode($chosen),
            'is_system' => false,
            'created_at_ms' => $now,
            'updated_at_ms' => $now,
        ];
        if ($hasRoleBizCol) {
            $payload['business_id'] = $businessId;
        }

        DB::table('roles')->insert($payload);

        return response()->json([
            'ok' => true,
            'id' => $id,
            'message' => "Role '{$name}' created.",
        ]);
    }

    public function updateRole(Request $request, string $id): JsonResponse
    {
        $businessId = trim((string) $request->input('businessId', ''));
        $ctx = $this->authorizeOwner($request, $businessId);
        if ($ctx instanceof JsonResponse) {
            return $ctx;
        }

        $role = DB::table('roles')->where('id', $id)->first();
        if (!$role) {
            return response()->json(['ok' => false, 'message' => 'Role not found.'], 404);
        }

        $hasRoleBizCol = Schema::hasColumn('roles', 'business_id');
        $roleBizId = $hasRoleBizCol ? (string) ($role->business_id ?? '') : '';
        $isSuperAdmin = $this->rbac->hasPermission($ctx['user'], 'manage_roles');

        if ($role->is_system || (!$isSuperAdmin && $roleBizId !== $businessId)) {
            return response()->json(['ok' => false, 'message' => 'Built-in system roles cannot be modified. Create a custom role instead.'], 403);
        }

        $name = trim((string) $request->input('name', $role->name));
        $description = trim((string) $request->input('description', (string) $role->description));
        $requestedPerms = $request->input('permissions', []);
        if ($name === '') {
            return response()->json(['ok' => false, 'message' => 'Role name is required.'], 422);
        }

        $allowedKeys = array_keys($this->allowedPermissionKeysForCaller($ctx['user']));
        $chosen = array_values(array_intersect($allowedKeys, is_array($requestedPerms) ? $requestedPerms : []));
        if (empty($chosen)) {
            $chosen = ['view_dashboard'];
        }

        DB::table('roles')->where('id', $id)->update([
            'name' => substr($name, 0, 100),
            'description' => substr($description, 0, 255),
            'permissions_json' => json_encode($chosen),
            'updated_at_ms' => $this->rbac->nowMs(),
        ]);

        return response()->json(['ok' => true, 'id' => $id, 'message' => "Role '{$name}' updated."]);
    }

    public function deleteRole(Request $request, string $id): JsonResponse
    {
        $businessId = trim((string) $request->query('businessId', (string) $request->input('businessId', '')));
        $ctx = $this->authorizeOwner($request, $businessId);
        if ($ctx instanceof JsonResponse) {
            return $ctx;
        }

        $role = DB::table('roles')->where('id', $id)->first();
        if (!$role) {
            return response()->json(['ok' => false, 'message' => 'Role not found.'], 404);
        }

        $hasRoleBizCol = Schema::hasColumn('roles', 'business_id');
        $roleBizId = $hasRoleBizCol ? (string) ($role->business_id ?? '') : '';
        $isSuperAdmin = $this->rbac->hasPermission($ctx['user'], 'manage_roles');
        if ($role->is_system || (!$isSuperAdmin && $roleBizId !== $businessId)) {
            return response()->json(['ok' => false, 'message' => 'Cannot delete built-in roles.'], 403);
        }

        if (DB::table('app_users')->where('role_id', $id)->exists()) {
            return response()->json(['ok' => false, 'message' => 'Reassign team members off this role before deleting it.'], 422);
        }

        DB::table('roles')->where('id', $id)->delete();
        return response()->json(['ok' => true, 'message' => 'Custom role deleted.']);
    }

    public function storeUser(Request $request): JsonResponse
    {
        $businessId = trim((string) $request->input('businessId', ''));
        $ctx = $this->authorizeOwner($request, $businessId);
        if ($ctx instanceof JsonResponse) {
            return $ctx;
        }

        $name = trim((string) $request->input('name', ''));
        $email = strtolower(trim((string) $request->input('email', '')));
        $password = (string) $request->input('password', '');
        $roleId = trim((string) $request->input('roleId', ''));
        $customRoleName = trim((string) $request->input('customRoleName', ''));
        $customPerms = $request->input('customPermissions', null);

        if ($name === '' || !filter_var($email, FILTER_VALIDATE_EMAIL)) {
            return response()->json(['ok' => false, 'message' => 'Valid full name and email are required.'], 422);
        }
        if (strlen($password) < 6) {
            return response()->json(['ok' => false, 'message' => 'Password must be at least 6 characters.'], 422);
        }
        if (DB::table('app_users')->where('email', $email)->exists()) {
            return response()->json(['ok' => false, 'message' => 'An account with this email already exists.'], 422);
        }

        $now = $this->rbac->nowMs();
        // If caller provided inline customPermissions (per-user feature assignment), create a dedicated custom role
        if (is_array($customPerms) && !empty($customPerms)) {
            $allowedKeys = array_keys($this->allowedPermissionKeysForCaller($ctx['user']));
            $chosen = array_values(array_intersect($allowedKeys, $customPerms));
            if (empty($chosen)) {
                $chosen = ['view_dashboard'];
            }
            $roleId = (string) Str::uuid();
            $rName = $customRoleName !== '' ? $customRoleName : ($name . ' Role');
            $rolePayload = [
                'id' => $roleId,
                'name' => substr($rName, 0, 100),
                'description' => "Custom access role for {$name}",
                'permissions_json' => json_encode($chosen),
                'is_system' => false,
                'created_at_ms' => $now,
                'updated_at_ms' => $now,
            ];
            if (Schema::hasColumn('roles', 'business_id')) {
                $rolePayload['business_id'] = $businessId;
            }
            DB::table('roles')->insert($rolePayload);
        } else {
            $role = DB::table('roles')->where('id', $roleId)->first();
            if (!$role || ($role->is_system && !$this->rbac->hasPermission($ctx['user'], 'manage_roles'))) {
                return response()->json(['ok' => false, 'message' => 'Selected role is not allowed.'], 422);
            }
        }

        $userId = (string) Str::uuid();
        DB::table('app_users')->insert([
            'id' => $userId,
            'name' => substr($name, 0, 150),
            'email' => $email,
            'password_hash' => Hash::make($password),
            'role_id' => $roleId,
            'business_ids_json' => json_encode([$businessId]),
            'is_active' => true,
            'last_login_ms' => 0,
            'created_at_ms' => $now,
            'updated_at_ms' => $now,
        ]);

        return response()->json([
            'ok' => true,
            'id' => $userId,
            'message' => "{$name} added to your business team. They can sign in now.",
        ]);
    }

    public function updateUser(Request $request, string $id): JsonResponse
    {
        $businessId = trim((string) $request->input('businessId', ''));
        $ctx = $this->authorizeOwner($request, $businessId);
        if ($ctx instanceof JsonResponse) {
            return $ctx;
        }

        if ($id === 'user-super-admin') {
            return response()->json(['ok' => false, 'message' => 'Cannot modify Super Admin.'], 403);
        }

        $target = DB::table('app_users')->where('id', $id)->first();
        if (!$target) {
            return response()->json(['ok' => false, 'message' => 'Team member not found.'], 404);
        }

        $bizIds = json_decode((string) $target->business_ids_json, true) ?: [];
        if (!in_array($businessId, array_map('strval', $bizIds), true)) {
            return response()->json(['ok' => false, 'message' => 'This user does not belong to your business.'], 403);
        }

        $name = trim((string) $request->input('name', $target->name));
        $email = strtolower(trim((string) $request->input('email', $target->email)));
        $password = (string) $request->input('password', '');
        $roleId = trim((string) $request->input('roleId', (string) $target->role_id));
        $customRoleName = trim((string) $request->input('customRoleName', ''));
        $customPerms = $request->input('customPermissions', null);

        if ($name === '' || !filter_var($email, FILTER_VALIDATE_EMAIL)) {
            return response()->json(['ok' => false, 'message' => 'Valid name and email are required.'], 422);
        }
        if (DB::table('app_users')->where('email', $email)->where('id', '!=', $id)->exists()) {
            return response()->json(['ok' => false, 'message' => 'Another account already uses this email.'], 422);
        }
        if ($password !== '' && strlen($password) < 6) {
            return response()->json(['ok' => false, 'message' => 'New password must be at least 6 characters.'], 422);
        }

        $now = $this->rbac->nowMs();
        if (is_array($customPerms) && !empty($customPerms)) {
            $allowedKeys = array_keys($this->allowedPermissionKeysForCaller($ctx['user']));
            $chosen = array_values(array_intersect($allowedKeys, $customPerms));
            if (empty($chosen)) {
                $chosen = ['view_dashboard'];
            }
            $roleId = (string) Str::uuid();
            $rName = $customRoleName !== '' ? $customRoleName : ($name . ' Role');
            $rolePayload = [
                'id' => $roleId,
                'name' => substr($rName, 0, 100),
                'description' => "Custom access role for {$name}",
                'permissions_json' => json_encode($chosen),
                'is_system' => false,
                'created_at_ms' => $now,
                'updated_at_ms' => $now,
            ];
            if (Schema::hasColumn('roles', 'business_id')) {
                $rolePayload['business_id'] = $businessId;
            }
            DB::table('roles')->insert($rolePayload);
        } else {
            $role = DB::table('roles')->where('id', $roleId)->first();
            if (!$role || ($role->is_system && !$this->rbac->hasPermission($ctx['user'], 'manage_roles'))) {
                return response()->json(['ok' => false, 'message' => 'Selected role is not allowed.'], 422);
            }
        }

        $update = [
            'name' => substr($name, 0, 150),
            'email' => $email,
            'role_id' => $roleId,
            'updated_at_ms' => $now,
        ];
        if ($password !== '') {
            $update['password_hash'] = Hash::make($password);
            DB::table('app_user_tokens')->where('user_id', $id)->delete();
        }

        DB::table('app_users')->where('id', $id)->update($update);

        return response()->json(['ok' => true, 'id' => $id, 'message' => "{$name} updated."]);
    }

    public function toggleUser(Request $request, string $id): JsonResponse
    {
        $businessId = trim((string) $request->input('businessId', ''));
        $ctx = $this->authorizeOwner($request, $businessId);
        if ($ctx instanceof JsonResponse) {
            return $ctx;
        }

        if ($id === 'user-super-admin' || $id === $ctx['user']['id']) {
            return response()->json(['ok' => false, 'message' => 'You cannot deactivate your own account.'], 422);
        }

        $target = DB::table('app_users')->where('id', $id)->first();
        if (!$target) {
            return response()->json(['ok' => false, 'message' => 'User not found.'], 404);
        }
        $bizIds = json_decode((string) $target->business_ids_json, true) ?: [];
        if (!in_array($businessId, array_map('strval', $bizIds), true)) {
            return response()->json(['ok' => false, 'message' => 'User does not belong to this business.'], 403);
        }

        $newActive = !$target->is_active;
        DB::table('app_users')->where('id', $id)->update([
            'is_active' => $newActive,
            'updated_at_ms' => $this->rbac->nowMs(),
        ]);
        if (!$newActive) {
            DB::table('app_user_tokens')->where('user_id', $id)->delete();
        }

        return response()->json([
            'ok' => true,
            'isActive' => $newActive,
            'message' => $newActive ? "{$target->name} activated." : "{$target->name} deactivated & signed out.",
        ]);
    }

    public function deleteUser(Request $request, string $id): JsonResponse
    {
        $businessId = trim((string) $request->query('businessId', (string) $request->input('businessId', '')));
        $ctx = $this->authorizeOwner($request, $businessId);
        if ($ctx instanceof JsonResponse) {
            return $ctx;
        }

        if ($id === 'user-super-admin' || $id === $ctx['user']['id']) {
            return response()->json(['ok' => false, 'message' => 'You cannot delete your own account.'], 422);
        }

        $target = DB::table('app_users')->where('id', $id)->first();
        if (!$target) {
            return response()->json(['ok' => false, 'message' => 'User not found.'], 404);
        }
        $bizIds = json_decode((string) $target->business_ids_json, true) ?: [];
        if (!in_array($businessId, array_map('strval', $bizIds), true)) {
            return response()->json(['ok' => false, 'message' => 'User does not belong to this business.'], 403);
        }

        DB::table('app_user_tokens')->where('user_id', $id)->delete();
        DB::table('app_users')->where('id', $id)->delete();

        return response()->json(['ok' => true, 'message' => "{$target->name} removed."]);
    }
}
