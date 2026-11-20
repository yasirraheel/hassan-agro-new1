<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration {
    public function up(): void
    {
        Schema::create('businesses', function (Blueprint $table) {
            $table->char('id', 36)->primary();
            $table->string('name');
            $table->string('tagline')->default('');
            $table->string('phone')->default('');
            $table->string('email')->default('');
            $table->text('address')->nullable();
            $table->string('tax_id')->default('');
            $table->string('invoice_prefix', 16)->default('HA');
            $table->decimal('default_tax_rate', 8, 2)->default(0);
            $table->text('invoice_footer')->nullable();
            $table->bigInteger('created_at_ms')->default(0);
            $table->bigInteger('updated_at_ms')->default(0)->index();
            $table->bigInteger('server_modified_at_ms')->default(0)->index();
            $table->boolean('is_deleted')->default(false);
        });

        Schema::create('products', function (Blueprint $table) {
            $table->char('id', 36)->primary();
            $table->char('business_id', 36)->index();
            $table->string('name');
            $table->string('brand')->default('');
            $table->string('category', 64)->default('Insecticide');
            $table->string('pack_size', 64)->default('');
            $table->string('barcode', 128)->default('')->index();
            $table->string('batch_number', 128)->default('');
            $table->bigInteger('expiry_date_millis')->default(0);
            $table->integer('stock_qty')->default(0);
            $table->integer('low_stock_threshold')->default(10);
            $table->decimal('purchase_price', 14, 2)->default(0);
            $table->decimal('mrp_price', 14, 2)->default(0);
            $table->decimal('distributor_price', 14, 2)->default(0);
            $table->decimal('wholesaler_price', 14, 2)->default(0);
            $table->decimal('retail_price', 14, 2)->default(0);
            $table->bigInteger('updated_at_ms')->default(0)->index();
            $table->bigInteger('server_modified_at_ms')->default(0)->index();
            $table->boolean('is_deleted')->default(false);
        });

        Schema::create('invoices', function (Blueprint $table) {
            $table->char('id', 36)->primary();
            $table->char('business_id', 36)->index();
            $table->string('invoice_number', 64)->index();
            $table->string('customer_name');
            $table->string('customer_phone', 64)->default('');
            $table->string('customer_email')->default('');
            $table->string('pricing_tier', 32)->default('RETAIL');
            $table->decimal('subtotal', 14, 2)->default(0);
            $table->decimal('discount_amount', 14, 2)->default(0);
            $table->boolean('tax_enabled')->default(false);
            $table->decimal('tax_rate_percent', 8, 2)->default(0);
            $table->decimal('tax_amount', 14, 2)->default(0);
            $table->decimal('grand_total', 14, 2)->default(0);
            $table->string('payment_method', 64)->default('Cash');
            $table->string('payment_status', 32)->default('PAID');
            $table->decimal('amount_paid', 14, 2)->default(0);
            $table->decimal('balance_due', 14, 2)->default(0);
            $table->boolean('is_returned')->default(false);
            $table->text('notes')->nullable();
            $table->bigInteger('created_at_ms')->default(0);
            $table->bigInteger('updated_at_ms')->default(0)->index();
            $table->bigInteger('server_modified_at_ms')->default(0)->index();
            $table->boolean('is_deleted')->default(false);
        });

        Schema::create('invoice_items', function (Blueprint $table) {
            $table->char('id', 36)->primary();
            $table->char('invoice_id', 36)->index();
            $table->char('product_id', 36)->default('');
            $table->string('product_name');
            $table->string('pack_size', 64)->default('');
            $table->string('batch_number', 128)->default('');
            $table->integer('quantity')->default(1);
            $table->decimal('unit_price', 14, 2)->default(0);
            $table->decimal('line_total', 14, 2)->default(0);
        });

        Schema::create('expenses', function (Blueprint $table) {
            $table->char('id', 36)->primary();
            $table->char('business_id', 36)->index();
            $table->string('title');
            $table->string('category', 64)->default('Other');
            $table->decimal('amount', 14, 2)->default(0);
            $table->string('payment_method', 64)->default('Cash');
            $table->string('payee')->default('');
            $table->text('notes')->nullable();
            $table->bigInteger('expense_date_millis')->default(0);
            $table->bigInteger('updated_at_ms')->default(0)->index();
            $table->bigInteger('server_modified_at_ms')->default(0)->index();
            $table->boolean('is_deleted')->default(false);
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('expenses');
        Schema::dropIfExists('invoice_items');
        Schema::dropIfExists('invoices');
        Schema::dropIfExists('products');
        Schema::dropIfExists('businesses');
    }
};
