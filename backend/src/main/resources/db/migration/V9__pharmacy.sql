-- MedFlow V9: pharmacy catalog, batch inventory, dispensing/returns and reconciliation.

ALTER TABLE drug
    ADD COLUMN generic_name VARCHAR(100) NULL AFTER name,
    ADD COLUMN manufacturer VARCHAR(100) NULL AFTER dosage_form,
    ADD COLUMN category VARCHAR(16) NOT NULL DEFAULT 'WESTERN' AFTER manufacturer;

ALTER TABLE stock_reservation
    ADD COLUMN returned_qty INT NOT NULL DEFAULT 0 AFTER quantity,
    ADD CONSTRAINT chk_reservation_return CHECK (returned_qty >= 0 AND returned_qty <= quantity);

INSERT INTO sys_permission (id, code, name, type, route_path, icon, sort_no) VALUES
    (54, 'menu:inventory', '药房库存', 'MENU', '/pharmacy/inventory', 'package', 46);

INSERT INTO sys_role_permission (role_id, permission_id) VALUES (4, 54);
