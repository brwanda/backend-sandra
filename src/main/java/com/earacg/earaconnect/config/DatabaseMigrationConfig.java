package com.earacg.earaconnect.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

@Component
public class DatabaseMigrationConfig implements CommandLineRunner {

    @Autowired
    private DataSource dataSource;

    @Override
    public void run(String... args) throws Exception {
        updateRoleConstraint();
    }

    private void updateRoleConstraint() {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            
            // Drop existing constraint if it exists
            statement.execute("ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check");
            
            // Add new constraint with all role values
            String constraintSql = "ALTER TABLE users ADD CONSTRAINT users_role_check " +
                    "CHECK (role IN ('ADMIN', 'SECRETARY', 'CHAIR', 'VICE_CHAIR', 'HOD', " +
                    "'COMMISSIONER_GENERAL', 'SUBCOMMITTEE_MEMBER', 'DELEGATION_SECRETARY', " +
                    "'COMMITTEE_SECRETARY', 'COMMITTEE_MEMBER'))";
            
            statement.execute(constraintSql);
            
            System.out.println("✅ Database role constraint updated successfully!");
            
        } catch (Exception e) {
            System.err.println("❌ Error updating database constraint: " + e.getMessage());
            // Don't throw the exception to allow the application to continue
        }
    }
} 