-- Create plans table for managing task execution plans
CREATE TABLE IF NOT EXISTS plans (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    sub_task_id BIGINT NOT NULL,
    created_by BIGINT NOT NULL,
    start_date DATETIME,
    end_date DATETIME,
    status VARCHAR(50) DEFAULT 'DRAFT',
    priority VARCHAR(50) DEFAULT 'MEDIUM',
    progress_percentage INT DEFAULT 0,
    notes TEXT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME,
    
    CONSTRAINT fk_plan_subtask FOREIGN KEY (sub_task_id) REFERENCES sub_tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_plan_creator FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_progress CHECK (progress_percentage >= 0 AND progress_percentage <= 100)
);

-- Create indexes for better query performance
CREATE INDEX idx_plans_subtask ON plans(sub_task_id);
CREATE INDEX idx_plans_creator ON plans(created_by);
CREATE INDEX idx_plans_status ON plans(status);
CREATE INDEX idx_plans_priority ON plans(priority);
