<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        if (!Schema::hasTable('customers')) {
            Schema::create('customers', function (Blueprint $table) {
                $table->char('id', 36)->primary();
                $table->char('business_id', 36)->index();
                $table->string('name');
                $table->string('phone', 64)->default('')->index();
                $table->string('email')->default('');
                $table->text('address')->nullable();
                $table->string('pricing_tier', 32)->default('RETAIL');
                $table->decimal('credit_limit', 14, 2)->default(0);
                $table->decimal('opening_balance', 14, 2)->default(0);
                $table->text('notes')->nullable();
                $table->char('performed_by_id', 36)->default('');
                $table->string('performed_by_name', 150)->default('');
                $table->bigInteger('created_at_ms')->default(0);
                $table->bigInteger('updated_at_ms')->default(0)->index();
                $table->bigInteger('server_modified_at_ms')->default(0)->index();
                $table->boolean('is_deleted')->default(false);
            });
        }

        if (Schema::hasTable('invoices') && !Schema::hasColumn('invoices', 'customer_id')) {
            Schema::table('invoices', function (Blueprint $table) {
                $table->char('customer_id', 36)->default('')->after('invoice_number')->index();
            });
        }

        if (Schema::hasTable('businesses')) {
            Schema::table('businesses', function (Blueprint $table) {
                if (!Schema::hasColumn('businesses', 'tax_presets_json')) {
                    $table->text('tax_presets_json')->nullable()->after('pricing_tier_labels_json');
                }
                if (!Schema::hasColumn('businesses', 'pricing_rules_json')) {
                    $table->text('pricing_rules_json')->nullable()->after('tax_presets_json');
                }
            });
        }

        if (Schema::hasTable('roles') && !Schema::hasColumn('roles', 'business_id')) {
            Schema::table('roles', function (Blueprint $table) {
                $table->char('business_id', 36)->nullable()->after('id')->index();
            });
            try {
                DB::statement('ALTER TABLE `roles` DROP INDEX `roles_name_unique`');
            } catch (\Throwable $e) {
                // Index may be named uq_roles_name or not exist; ignore safely
            }
            try {
                DB::statement('ALTER TABLE `roles` DROP INDEX `uq_roles_name`');
            } catch (\Throwable $e) {
                // Ignore if absent
            }
        }
    }

    public function down(): void
    {
        Schema::dropIfExists('customers');

        if (Schema::hasTable('invoices') && Schema::hasColumn('invoices', 'customer_id')) {
            Schema::table('invoices', function (Blueprint $table) {
                $table->dropColumn('customer_id');
            });
        }

        if (Schema::hasTable('businesses')) {
            Schema::table('businesses', function (Blueprint $table) {
                if (Schema::hasColumn('businesses', 'tax_presets_json')) {
                    $table->dropColumn('tax_presets_json');
                }
                if (Schema::hasColumn('businesses', 'pricing_rules_json')) {
                    $table->dropColumn('pricing_rules_json');
                }
            });
        }

        if (Schema::hasTable('roles') && Schema::hasColumn('roles', 'business_id')) {
            Schema::table('roles', function (Blueprint $table) {
                $table->dropColumn('business_id');
            });
        }
    }
};
