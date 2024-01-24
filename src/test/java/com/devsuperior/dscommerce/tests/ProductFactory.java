package com.devsuperior.dscommerce.tests;

import com.devsuperior.dscommerce.entities.Category;
import com.devsuperior.dscommerce.entities.Product;

public class ProductFactory {

    public static Product createProducts() {

        Category category = CategoryFactory.createCategory();
        Product product = new Product(1L, "Console Playstation", "consectetur adipiscing elit", 3999.0, "https://raw.githubusercontent.com/devsuperior/dscatalog-resources/master/backend/img/9-big.jpg");
        product.getCategories().add(category);
        return product;
    }

    public static Product createProducts(String name) {
        Product product = createProducts();
        product.setName(name);
        return product;
    }

}
