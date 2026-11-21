<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        if (Schema::hasTable('invoice_items') && !Schema::hasColumn('invoice_items', 'cost_price')) {
            Schema::table('invoice_items', function (Blueprint $table) {
                $table->decimal('cost_price', 14, 2)->default(0)->after('unit_price');
            });
        }

        if (Schema::hasTable('invoices') && !Schema::hasColumn('invoices', 'proof_image_path')) {
            Schema::table('invoices', function (Blueprint $table) {
                $table->text('proof_image_path')->nullable()->after('notes');
            });
        }

        if (Schema::hasTable('expenses') && !Schema::hasColumn('expenses', 'proof_image_path')) {
            Schema::table('expenses', function (Blueprint $table) {
                $table->text('proof_image_path')->nullable()->after('notes');
            });
        }
    }

    public function down(): void
    {
        if (Schema::hasTable('invoice_items') && Schema::hasColumn('invoice_items', 'cost_price')) {
            Schema::table('invoice_items', function (Blueprint $table) {
                $table->dropColumn('cost_price');
            });
        }
        if (Schema::hasTable('invoices') && Schema::hasColumn('invoices', 'proof_image_path')) {
            Schema::table('invoices', function (Blueprint $table) {
                $table->dropColumn('proof_image_path');
            });
        }
        if (Schema::hasTable('expenses') && Schema::hasColumn('expenses', 'proof_image_path')) {
            Schema::table('expenses', function (Blueprint $table) {
                $table->dropColumn('proof_image_path');
            });
        }
    }
};
