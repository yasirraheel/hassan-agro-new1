<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration {
    public function up(): void
    {
        if (!Schema::hasTable('roles')) {
            Schema::create('roles', function (Blueprint $table) {
                $table->char('id', 36)->primary();
                $table->string('name', 100)->unique();
                $table->string('description', 255)->default('');
                $table->text('permissions_json');
                $table->boolean('is_system')->default(false);
                $table->bigInteger('created_at_ms')->default(0);
                $table->bigInteger('updated_at_ms')->default(0);
            });
        }

        if (!Schema::hasTable('app_users')) {
            Schema::create('app_users', function (Blueprint $table) {
                $table->char('id', 36)->primary();
                $table->string('name', 150);
                $table->string('email', 191)->unique();
                $table->string('password_hash', 255);
                $table->char('role_id', 36)->index();
                $table->text('business_ids_json');
                $table->boolean('is_active')->default(true);
                $table->bigInteger('last_login_ms')->default(0);
                $table->bigInteger('created_at_ms')->default(0);
                $table->bigInteger('updated_at_ms')->default(0);
            });
        }

        if (!Schema::hasTable('app_user_tokens')) {
            Schema::create('app_user_tokens', function (Blueprint $table) {
                $table->char('id', 36)->primary();
                $table->char('user_id', 36)->index();
                $table->char('token_hash', 64)->unique();
                $table->string('device_id', 100)->default('');
                $table->bigInteger('created_at_ms')->default(0);
                $table->bigInteger('last_used_ms')->default(0);
            });
        }

        if (Schema::hasTable('businesses')) {
            $bizCols = [
                'theme_key' => fn (Blueprint $t) => $t->string('theme_key', 32)->default('OCEAN'),
                'business_type' => fn (Blueprint $t) => $t->string('business_type', 32)->default('GENERAL'),
                'currency_symbol' => fn (Blueprint $t) => $t->string('currency_symbol', 12)->default('Rs.'),
                'track_expiry' => fn (Blueprint $t) => $t->boolean('track_expiry')->default(true),
                'track_batch' => fn (Blueprint $t) => $t->boolean('track_batch')->default(true),
                'batch_field_label' => fn (Blueprint $t) => $t->string('batch_field_label', 64)->default('Batch No.'),
                'pack_size_label' => fn (Blueprint $t) => $t->string('pack_size_label', 64)->default('Pack / Size'),
                'product_categories_json' => fn (Blueprint $t) => $t->text('product_categories_json')->nullable(),
                'expense_categories_json' => fn (Blueprint $t) => $t->text('expense_categories_json')->nullable(),
                'payment_methods_json' => fn (Blueprint $t) => $t->text('payment_methods_json')->nullable(),
                'pricing_tier_labels_json' => fn (Blueprint $t) => $t->text('pricing_tier_labels_json')->nullable(),
            ];
            foreach ($bizCols as $col => $adder) {
                if (!Schema::hasColumn('businesses', $col)) {
                    Schema::table('businesses', $adder);
                }
            }
        }

        foreach (['products', 'invoices', 'expenses'] as $tbl) {
            if (Schema::hasTable($tbl)) {
                if (!Schema::hasColumn($tbl, 'performed_by_id')) {
                    Schema::table($tbl, function (Blueprint $table) {
                        $table->char('performed_by_id', 36)->default('');
                    });
                }
                if (!Schema::hasColumn($tbl, 'performed_by_name')) {
                    Schema::table($tbl, function (Blueprint $table) {
                        $table->string('performed_by_name', 150)->default('');
                    });
                }
            }
        }
    }

    public function down(): void
    {
        Schema::dropIfExists('app_user_tokens');
        Schema::dropIfExists('app_users');
        Schema::dropIfExists('roles');
    }
};
