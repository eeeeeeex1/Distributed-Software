// API基础URL
const API_BASE_URL = 'http://localhost:8080/api';

// 检查登录状态
function checkLoginStatus() {
    const user = localStorage.getItem('user');
    const userInfo = document.getElementById('user-info');
    const authButtons = document.getElementById('auth-buttons');
    
    if (user) {
        const userData = JSON.parse(user);
        if (userInfo) {
            userInfo.style.display = 'inline';
            document.getElementById('username').textContent = userData.username;
        }
        if (authButtons) {
            authButtons.style.display = 'none';
        }
    } else {
        if (userInfo) {
            userInfo.style.display = 'none';
        }
        if (authButtons) {
            authButtons.style.display = 'inline';
        }
    }
}

// 用户登录
function handleLogin(event) {
    event.preventDefault();
    
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;
    const messageDiv = document.getElementById('message');
    
    fetch(`${API_BASE_URL}/user/login`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ username, password })
    })
    .then(response => response.json())
    .then(data => {
        if (data.code === 200) {
            // 登录成功
            localStorage.setItem('user', JSON.stringify(data.user));
            showMessage(messageDiv, '登录成功！正在跳转...', 'success');
            setTimeout(() => {
                window.location.href = 'index.html';
            }, 1500);
        } else {
            showMessage(messageDiv, data.msg || '登录失败', 'error');
        }
    })
    .catch(error => {
        showMessage(messageDiv, '网络错误，请稍后重试', 'error');
        console.error('Login error:', error);
    });
    
    return false;
}

// 用户注册
function handleRegister(event) {
    event.preventDefault();
    
    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;
    const confirmPassword = document.getElementById('confirm-password').value;
    const messageDiv = document.getElementById('message');
    
    // 验证密码
    if (password !== confirmPassword) {
        showMessage(messageDiv, '两次输入的密码不一致', 'error');
        return false;
    }
    
    fetch(`${API_BASE_URL}/user/register`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({ username, password })
    })
    .then(response => response.json())
    .then(data => {
        if (data.code === 200) {
            showMessage(messageDiv, '注册成功！正在跳转到登录页面...', 'success');
            setTimeout(() => {
                window.location.href = 'login.html';
            }, 1500);
        } else {
            showMessage(messageDiv, data.msg || '注册失败', 'error');
        }
    })
    .catch(error => {
        showMessage(messageDiv, '网络错误，请稍后重试', 'error');
        console.error('Register error:', error);
    });
    
    return false;
}

// 用户退出
function logout() {
    localStorage.removeItem('user');
    window.location.href = 'index.html';
}

// 显示消息
function showMessage(element, message, type) {
    element.textContent = message;
    element.className = `message ${type}`;
    element.style.display = 'block';
    
    // 3秒后自动隐藏
    setTimeout(() => {
        element.style.display = 'none';
    }, 3000);
}

// 加载热门商品
function loadHotProducts() {
    const container = document.getElementById('hot-products');
    if (!container) return;
    
    // 模拟加载热门商品（ID 1-6）
    const productIds = [1, 2, 3, 4, 5, 6];
    container.innerHTML = '<div class="loading">加载中...</div>';
    
    Promise.all(productIds.map(id => 
        fetch(`${API_BASE_URL}/product/detail/${id}`)
            .then(response => response.json())
            .catch(() => null)
    ))
    .then(products => {
        container.innerHTML = '';
        products.forEach(data => {
            if (data && data.code === 200 && data.data) {
                container.appendChild(createProductCard(data.data));
            }
        });
        
        if (container.innerHTML === '') {
            container.innerHTML = '<div class="loading">暂无商品数据</div>';
        }
    })
    .catch(error => {
        container.innerHTML = '<div class="loading">加载失败，请稍后重试</div>';
        console.error('Load products error:', error);
    });
}

// 创建商品卡片
function createProductCard(product) {
    const card = document.createElement('div');
    card.className = 'product-card';
    
    card.innerHTML = `
        <div class="product-image">
            <img src="https://trae-api-cn.mchost.guru/api/ide/v1/text_to_image?prompt=product%20image%20${encodeURIComponent(product.name)}&image_size=square" alt="${product.name}">
        </div>
        <div class="product-info">
            <h3 class="product-name">${product.name}</h3>
            <p class="product-desc">${product.desc}</p>
            <div class="product-price">
                <span class="price">¥${product.price}</span>
                <span class="stock">库存: ${product.stock}</span>
            </div>
            <button class="btn btn-primary">立即抢购</button>
        </div>
    `;
    
    return card;
}

// 页面加载完成后执行
window.onload = function() {
    checkLoginStatus();
    
    // 加载热门商品（仅在首页）
    if (window.location.pathname.includes('index.html') || window.location.pathname === '/') {
        loadHotProducts();
    }
};