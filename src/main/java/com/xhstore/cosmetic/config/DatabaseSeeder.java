package com.xhstore.cosmetic.config;

import com.xhstore.cosmetic.model.*;
import com.xhstore.cosmetic.repository.*;
import org.mindrot.jbcrypt.BCrypt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * DatabaseSeeder — tạo dữ liệu mẫu ban đầu khi DB còn trống.
 * Chỉ chạy 1 lần khi chưa có user nào trong DB.
 */
@Component
public class DatabaseSeeder implements CommandLineRunner {

    @Autowired private UserRepository userRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private ProductRepository productRepository;
    @Autowired private VoucherRepository voucherRepository;
    @Autowired private BoothConfigRepository boothConfigRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderItemRepository orderItemRepository;

    @Override
    public void run(String... args) {
        seedUsers();
        List<Category> categories = seedCategories();
        seedProducts(categories);
        seedVouchers();
        seedBooths();
        seedOrders();
    }

    private void seedUsers() {
        // Chỉ seed nếu DB chưa có tài khoản nào
        if (userRepository.findByUsername("admin").isPresent()) return;

        User admin = new User();
        admin.setUsername("admin");
        admin.setPassword(BCrypt.hashpw("admin123", BCrypt.gensalt()));
        admin.setEmail("admin@xhstore.vn");
        admin.setPhone("0999999999");
        admin.setAddress("VN");
        admin.setRole("ADMIN");
        userRepository.save(admin);

        User client = new User();
        client.setUsername("user");
        client.setPassword(BCrypt.hashpw("user123", BCrypt.gensalt()));
        client.setEmail("khachhang@gmail.com");
        client.setPhone("0901234567");
        client.setAddress("HN");
        client.setRole("CLIENT");
        userRepository.save(client);

        System.out.println("[Seeder] Đã tạo tài khoản admin và user mẫu.");
    }

    private List<Category> seedCategories() {
        if (categoryRepository.count() > 0) return categoryRepository.findAll();

        List<Category> cats = categoryRepository.saveAll(List.of(
            new Category(null, "Thời Trang", "fa-solid fa-shirt"),
            new Category(null, "Mỹ Phẩm", "fa-solid fa-star"),
            new Category(null, "Trang Sức", "fa-solid fa-gem"),
            new Category(null, "Phụ Kiện", "fa-solid fa-bag-shopping"),
            new Category(null, "Chăm Sóc Da", "fa-solid fa-droplet")
        ));
        System.out.println("[Seeder] Đã tạo 5 danh mục.");
        return cats;
    }

    private void seedProducts(List<Category> categories) {
        if (productRepository.count() > 0) return;

        Category fashion = categories.get(0);
        Category beauty = categories.get(1);
        Category jewelry = categories.get(2);
        Category accessories = categories.get(3);
        Category skincare = categories.get(4);

        List<Product> products = List.of(
            buildProduct(
                "Áo Blazer Nữ Bẹt Vai Dáng Suông Công Sở",
                "Áo blazer dáng suông tinh tế, chất liệu đứng dáng sang trọng, phù hợp công sở thanh lịch.",
                new BigDecimal("285000"), new BigDecimal("380000"), "images/office_fashion_set_1779642635158.png",
                fashion, 150, 0, 0.0
            ),
            buildProduct(
                "Chân Váy Bút Chì Công Sở Lưng Cao Dáng Ôm",
                "Chân váy bút chì lưng cao tôn dáng, chất liệu co giãn nhẹ tạo sự thoải mái tối đa cho ngày dài làm việc.",
                new BigDecimal("195000"), new BigDecimal("260000"), "images/office_fashion_set_1779642635158.png",
                fashion, 80, 0, 0.0
            ),
            buildProduct(
                "Sơ Mi Trắng Lụa Mango Cổ V Công Sở Thanh Lịch",
                "Sơ mi lụa mềm mại, mát mẻ, chống nhăn cực tốt cho ngày bận rộn.",
                new BigDecimal("149000"), new BigDecimal("199000"), "images/office_fashion_set_1779642635158.png",
                fashion, 200, 0, 0.0
            ),
            buildProduct(
                "Quần Âu Nữ Lưng Cao Ống Suông Vải Tuyết Mưa",
                "Quần âu nữ ống suông trẻ trung hiện đại, che khuyết điểm chân rất tốt.",
                new BigDecimal("229000"), new BigDecimal("299000"), "images/office_fashion_set_1779642635158.png",
                fashion, 120, 0, 0.0
            ),

            buildProduct(
                "Kem Nền BB Cream SPF50 Kiềm Dầu Che Phủ Tốt",
                "Kem nền BB Cream thế hệ mới, che phủ thâm mụn tối đa, giữ tone 8 tiếng.",
                new BigDecimal("99000"), new BigDecimal("135000"), "images/cosmetics_office_set_1779642650180.png",
                beauty, 300, 0, 0.0
            ),
            buildProduct(
                "Son Dưỡng Có Màu Cam Đào Tự Nhiên 3.5g",
                "Son dưỡng có màu cam đào trẻ trung, cấp ẩm sâu, làm hồng môi tự nhiên.",
                new BigDecimal("85000"), new BigDecimal("115000"), "images/cosmetic_lipstick_1779182582401.png",
                beauty, 250, 0, 0.0
            ),

            buildProduct(
                "Dây Chuyền Mạ Vàng 18K Mặt Ngôi Sao Tinh Tế",
                "Dây chuyền vàng mỏng đính mặt ngôi sao nhỏ xinh xắn, tôn làn da của nàng.",
                new BigDecimal("89000"), new BigDecimal("120000"), "images/luxury_necklace_1779182619976.png",
                jewelry, 150, 0, 0.0
            ),
            buildProduct(
                "Bông Tai Khuyên Tròn Inox Chống Gỉ Công Sở",
                "Khuyên tai tròn tối giản nhưng không kém phần thu hút cho phong cách hàng ngày.",
                new BigDecimal("55000"), new BigDecimal("75000"), "images/accessories_jewelry_set_1779642671042.png",
                jewelry, 180, 0, 0.0
            ),
            buildProduct(
                "Túi Tote Da PU Đựng Laptop Văn Phòng Sang Trọng",
                "Túi tote da PU cỡ lớn đựng vừa laptop 14 inch, tài liệu a4 cho nàng công sở.",
                new BigDecimal("245000"), new BigDecimal("320000"), "images/office_bag_tote_1779642706379.png",
                accessories, 90, 0, 0.0
            ),
            buildProduct(
                "Vòng Tay Đá Tự Nhiên Mix Hạt May Mắn Bình An",
                "Vòng tay phong thủy tinh tế mang lại năng lượng tích cực cho công việc.",
                new BigDecimal("65000"), new BigDecimal("85000"), "images/gold_ring_1779182641831.png",
                accessories, 140, 0, 0.0
            ),

            // ---- CHĂM SÓC DA ----
            buildProduct(
                "Bộ Dưỡng Da 3 Bước Toner + Serum + Kem Dưỡng",
                "Routine tối giản giúp làm dịu và phục hồi da dầu mụn nhạy cảm.",
                new BigDecimal("185000"), new BigDecimal("250000"), "images/skincare_routine_set_1779642719171.png",
                skincare, 110, 0, 0.0
            ),
            buildProduct(
                "Serum Vitamin C 20% Sáng Da Chống Lão Hóa 30ml",
                "Tinh chất serum mờ thâm mụn sáng da rõ rệt sau 14 ngày sử dụng.",
                new BigDecimal("145000"), new BigDecimal("195000"), "images/skincare_serum_1779182603117.png",
                skincare, 95, 0, 0.0
            )
        );

        productRepository.saveAll(products);
        System.out.println("[Seeder] Đã tạo " + products.size() + " sản phẩm mẫu.");
    }

    private void seedVouchers() {
        if (voucherRepository.count() > 0) return;

        // Voucher giảm theo %
        Voucher v1 = new Voucher();
        v1.setCode("MYPHAM10");
        v1.setDiscountType("PERCENT");
        v1.setDiscountValue(new BigDecimal("10")); // Giảm 10%
        v1.setMinOrderValue(new BigDecimal("200000"));
        v1.setMaxUses(100);
        v1.setExpiryDate(LocalDate.now().plusMonths(6));
        v1.setIsActive(true);

        // Voucher giảm tiền cố định
        Voucher v2 = new Voucher();
        v2.setCode("XINCHAO50");
        v2.setDiscountType("FIXED");
        v2.setDiscountValue(new BigDecimal("50000")); // Giảm 50.000đ
        v2.setMinOrderValue(new BigDecimal("400000"));
        v2.setMaxUses(200);
        v2.setExpiryDate(LocalDate.now().plusMonths(3));
        v2.setIsActive(true);

        voucherRepository.saveAll(List.of(v1, v2));
        System.out.println("[Seeder] Đã tạo voucher mẫu: MYPHAM10, XINCHAO50.");
    }

    private void seedBooths() {
        if (boothConfigRepository.count() > 0) return;

        boothConfigRepository.saveAll(List.of(
            new BoothConfig(
                null, "office", "Công Sở Chuyên Nghiệp",
                "Trang phục tinh tế, lịch lãm cho nàng văn phòng", "-20%",
                "New Arrival", "Bộ sưu tập Hè",
                "images/office_fashion_set_1779642635158.png"
            ),
            new BoothConfig(
                null, "beauty", "Làm Đẹp Mỗi Ngày",
                "Mỹ phẩm, skincare Hàn Quốc chất lượng cao giá tốt", "-30%",
                "Best Seller", "Chăm da tại nhà",
                "images/cosmetics_office_set_1779642650180.png"
            ),
            new BoothConfig(
                null, "jewel", "Trang Sức & Phụ Kiện",
                "Tôn lên vẻ đẹp tinh tế với phụ kiện thời thượng", "-15%",
                "Premium", "Trang sức tinh tế",
                "images/accessories_jewelry_set_1779642671042.png"
            )
        ));
        System.out.println("[Seeder] Đã tạo 3 cấu hình gian hàng mặc định.");
    }

    private Product buildProduct(String name, String desc, BigDecimal price, BigDecimal originalPrice,
                                  String imageUrl, Category category,
                                  int stock, int soldCount, double rating) {
        Product p = new Product();
        p.setName(name);
        p.setDescription(desc);
        p.setPrice(price);
        p.setOriginalPrice(originalPrice);
        p.setImageUrl(imageUrl);
        p.setCategory(category);
        p.setStock(stock);
        p.setSoldCount(soldCount);
        p.setRating(rating);
        p.setIsActive(true);
        return p;
    }

    private void seedOrders() {
        if (orderRepository.count() > 0) return;

        User client = userRepository.findByUsername("user").orElse(null);
        if (client == null) return;

        List<Product> products = productRepository.findAll();
        if (products.size() < 2) return;

        Product p1 = products.stream()
                .filter(p -> p.getName().contains("Kem Nền"))
                .findFirst()
                .orElse(products.get(0));

        Product p2 = products.stream()
                .filter(p -> p.getName().contains("Son Dưỡng"))
                .findFirst()
                .orElse(products.get(Math.min(1, products.size() - 1)));

        BigDecimal amount = p1.getPrice().add(p2.getPrice());

        // Tạo 1 đơn hàng trạng thái DELIVERED cho 'user' để đánh giá thử nghiệm
        Order order = new Order();
        order.setUser(client);
        order.setOrderCode("DH" + (System.currentTimeMillis() % 1000000));
        order.setReceiverName("XH");
        order.setReceiverPhone("0901234567");
        order.setReceiverAddress("VN");
        order.setPaymentMethod("COD");
        order.setStatus("DELIVERED");
        order.setTotalAmount(amount);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setFinalAmount(amount);
        order.setCreatedAt(java.time.LocalDateTime.now());

        Order savedOrder = orderRepository.save(order);

        OrderItem item1 = new OrderItem();
        item1.setOrder(savedOrder);
        item1.setProduct(p1);
        item1.setProductName(p1.getName());
        item1.setProductPrice(p1.getPrice());
        item1.setQuantity(1);
        item1.setImageUrl(p1.getImageUrl());

        OrderItem item2 = new OrderItem();
        item2.setOrder(savedOrder);
        item2.setProduct(p2);
        item2.setProductName(p2.getName());
        item2.setProductPrice(p2.getPrice());
        item2.setQuantity(1);
        item2.setImageUrl(p2.getImageUrl());

        orderItemRepository.saveAll(List.of(item1, item2));
        System.out.println("[Seeder] Đã tạo đơn hàng DELIVERED mẫu cho 'user' để thử nghiệm đánh giá.");
    }
}
