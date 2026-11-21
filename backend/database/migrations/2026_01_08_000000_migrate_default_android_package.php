<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        if (!Schema::hasTable('authorized_apps')) {
            return;
        }

        $old = 'com.aistudio.hassanagro.k7m2px';
        $new = 'com.business.manager';
        $legacy = DB::table('authorized_apps')->where('package_name', $old)->first();
        $current = DB::table('authorized_apps')->where('package_name', $new)->first();

        if ($legacy && !$current) {
            DB::table('authorized_apps')->where('id', $legacy->id)->update([
                'package_name' => $new,
                'name' => 'Business Manager Android',
                'updated_at_ms' => (int) round(microtime(true) * 1000),
            ]);
        } elseif ($legacy && $current) {
            // Keep the new registration and disable the retired package so old APKs
            // cannot remain an active client after the package rename.
            DB::table('authorized_apps')->where('id', $legacy->id)->update([
                'is_active' => false,
                'updated_at_ms' => (int) round(microtime(true) * 1000),
            ]);
        }
    }

    public function down(): void
    {
        if (!Schema::hasTable('authorized_apps')) {
            return;
        }

        $current = DB::table('authorized_apps')->where('package_name', 'com.business.manager')->first();
        if ($current && !DB::table('authorized_apps')->where('package_name', 'com.aistudio.hassanagro.k7m2px')->exists()) {
            DB::table('authorized_apps')->where('id', $current->id)->update([
                'package_name' => 'com.aistudio.hassanagro.k7m2px',
                'updated_at_ms' => (int) round(microtime(true) * 1000),
            ]);
        }
    }
};
