DROP TABLE IF EXISTS booking;
DROP TABLE IF EXISTS car_inventory;

CREATE TABLE IF NOT EXISTS car_inventory (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    car_segment VARCHAR(20) UNIQUE NOT NULL,
    total_cars INT NOT NULL
);

CREATE TABLE IF NOT EXISTS booking (
    booking_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_name VARCHAR(100) NOT NULL,
    age INT NOT NULL,
    license_number VARCHAR(30) NOT NULL,
    car_segment VARCHAR(20) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    rental_price DECIMAL(10,2) NOT NULL
);