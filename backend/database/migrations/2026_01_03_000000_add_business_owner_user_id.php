<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration {
    public function up(): void
    {
        if (Schema::hasTable('businesses') && !Schema::hasColumn('businesses', 'owner_user_id')) {
            Schema::table('businesses', function (Blueprint $table) {
                $table->char('owner_user_id', 36)->nullable()->index();
            });
        }
    }

    public function down(): void
    {
        if (Schema::hasTable('businesses') && Schema::hasColumn('businesses', 'owner_user_id')) {
            Schema::table('businesses', function (Blueprint $table) {
                $table->dropColumn('owner_user_id');
            });
        }
    }
};
