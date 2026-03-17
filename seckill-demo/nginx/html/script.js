/**
 * 获取商品详情（调用后端接口）
 */
function getProductDetail(id) {
    const detailBox = document.getElementById("detail-box");
    detailBox.innerHTML = "加载中...";

    fetch(`/api/product/detail/${id}`)
        .then(response => response.json())
        .then(data => {
            if (data.code === 200) {
                const product = data.data;
                detailBox.innerHTML = `
                    <h2>商品详情</h2>
                    <p>商品ID：${product.id}</p>
                    <p>商品名称：${product.name}</p>
                    <p>商品价格：¥${product.price}</p>
                    <p>商品库存：${product.stock}件</p>
                    <p>商品描述：${product.desc}</p>
                `;
            } else {
                detailBox.innerHTML = `<p style="color: red;">${data.msg}</p>`;
            }
        })
        .catch(error => {
            detailBox.innerHTML = `<p style="color: red;">请求失败：${error.message}</p>`;
        });
}