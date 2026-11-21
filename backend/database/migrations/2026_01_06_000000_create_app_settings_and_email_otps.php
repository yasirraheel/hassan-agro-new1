<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        if (!Schema::hasTable('app_settings')) {
            Schema::create('app_settings', function (Blueprint $table) {
                $table->string('key', 120)->primary();
                $table->text('value')->nullable();
                $table->bigInteger('updated_at_ms')->default(0);
            });
        }

        if (!Schema::hasTable('email_otps')) {
            Schema::create('email_otps', function (Blueprint $table) {
                $table->bigIncrements('id');
                $table->string('email', 191)->index();
                $table->string('code_hash', 255);
                $table->string('business_name', 200)->default('');
                $table->bigInteger('expires_at_ms')->default(0);
                $table->bigInteger('consumed_at_ms')->nullable();
                $table->integer('attempts')->default(0);
                $table->bigInteger('created_at_ms')->default(0);
            });
        }
    }

    public function down(): void
    {
        Schema::dropIfExists('email_otps');
        Schema::dropIfExists('app_settings');
    }
};
