-- 创建用户表
CREATE TABLE IF NOT EXISTS t_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    avatar VARCHAR(255),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建商品表
CREATE TABLE IF NOT EXISTS t_product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    title VARCHAR(255),
    price DECIMAL(10,2) NOT NULL,
    stock_count INT NOT NULL,
    img_url VARCHAR(255),
    status INT DEFAULT 0
);

-- 创建库存表
CREATE TABLE IF NOT EXISTS t_stock (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    lock_stock INT DEFAULT 0,
    actual_stock INT NOT NULL,
    FOREIGN KEY (product_id) REFERENCES t_product(id)
);

-- 创建订单表
CREATE TABLE IF NOT EXISTS t_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_no VARCHAR(50) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    status INT DEFAULT 0,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES t_user(id),
    FOREIGN KEY (product_id) REFERENCES t_product(id)
);

-- 插入测试数据
INSERT INTO t_user (username, password, phone) VALUES ('test', '123456', '13800138000');
INSERT INTO t_product (name, title, price, stock_count, img_url, status) VALUES ('测试商品', '测试商品标题', 99.99, 100, 'https://example.com/image.jpg', 1);
INSERT INTO t_stock (product_id, actual_stock) VALUES (1, 100);