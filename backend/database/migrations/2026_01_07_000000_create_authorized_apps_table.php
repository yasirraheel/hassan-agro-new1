<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        if (!Schema::hasTable('authorized_apps')) {
            Schema::create('authorized_apps', function (Blueprint $table) {
                $table->string('id', 64)->primary();
                $table->string('name', 120);
                $table->string('package_name', 191)->unique();
                $table->string('secret_key', 128);
                $table->string('cert_sha256', 128)->nullable();
                $table->string('last_seen_cert_sha256', 128)->nullable();
                $table->bigInteger('last_seen_at_ms')->default(0);
                $table->boolean('is_active')->default(true);
                $table->bigInteger('created_at_ms')->default(0);
                $table->bigInteger('updated_at_ms')->default(0);
            });
        }
    }

    public function down(): void
    {
        Schema::dropIfExists('authorized_apps');
    }
};
