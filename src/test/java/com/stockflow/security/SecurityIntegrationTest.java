package com.stockflow.security;

import java.util.List;
import java.util.Set;

import com.stockflow.auth.dto.LoginRequest;
import com.stockflow.auth.dto.LoginResponse;
import com.stockflow.auth.service.AuthService;
import com.stockflow.category.service.CategoryService;
import com.stockflow.customer.service.CustomerService;
import com.stockflow.inventory.service.InventoryService;
import com.stockflow.inventory.service.InventoryMovementService;
import com.stockflow.product.service.ProductService;
import com.stockflow.report.ReportService;
import com.stockflow.sale.service.SaleService;
import com.stockflow.stockentry.service.StockEntryService;
import com.stockflow.stockexit.service.StockExitService;
import com.stockflow.supplier.service.SupplierService;
import com.stockflow.user.dto.UserResponse;
import com.stockflow.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private JwtService jwtService;

	@MockitoBean
	private StockFlowUserDetailsService userDetailsService;

	@MockitoBean
	private CategoryService categoryService;

	@MockitoBean
	private SupplierService supplierService;

	@MockitoBean
	private CustomerService customerService;

	@MockitoBean
	private ProductService productService;

	@MockitoBean
	private InventoryService inventoryService;

	@MockitoBean
	private InventoryMovementService inventoryMovementService;

	@MockitoBean
	private SaleService saleService;

	@MockitoBean
	private StockEntryService stockEntryService;

	@MockitoBean
	private StockExitService stockExitService;

	@MockitoBean
	private CurrentUserService currentUserService;

	@MockitoBean
	private UserService userService;

	@MockitoBean
	private AuthService authService;

	@MockitoBean
	private ReportService reportService;

	@Test
	void swaggerDocumentationIsPublic() throws Exception {
		mockMvc.perform(get("/v3/api-docs")).andExpect(status().isOk());
		mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
	}

	@Test
	void healthEndpointIsPublicAndDoesNotExposeDetails() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"))
				.andExpect(jsonPath("$.components").doesNotExist());
	}

	@Test
	void reportPermissionsMatchBusinessRoles() throws Exception {
		mockMvc.perform(get("/api/reports/low-stock").with(user("manager").roles("INVENTORY_MANAGER")))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/reports/inventory-summary").with(user("seller").roles("SELLER")))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/reports/sales-summary").with(user("seller").roles("SELLER")))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/reports/low-stock").with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk());
	}

	@Test
	void protectedEndpointWithoutAuthenticationReturnsStandard401() throws Exception {
		mockMvc.perform(get("/api/products"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				.andExpect(jsonPath("$.error").value("Unauthorized"))
				.andExpect(jsonPath("$.message").value("Authentication is required"))
				.andExpect(jsonPath("$.path").value("/api/products"));
	}

	@Test
	void invalidBearerTokenReturns401InsteadOf500() throws Exception {
		mockMvc.perform(get("/api/products")
					.header("Authorization", "Bearer not-a-jwt"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401));
	}

	@Test
	void validBearerTokenAuthenticatesRequest() throws Exception {
		UserDetails principal = principal("ROLE_SELLER");
		String token = jwtService.generateToken(principal);
		when(userDetailsService.loadUserByUsername("user@example.com")).thenReturn(principal);
		when(categoryService.findAll()).thenReturn(List.of());

		mockMvc.perform(get("/api/categories")
					.header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());
	}

	@Test
	void adminCanManageCategoriesAndUsers() throws Exception {
		mockMvc.perform(post("/api/categories")
					.with(user("admin@example.com").roles("ADMIN"))
					.contentType(MediaType.APPLICATION_JSON)
					.content(categoryJson()))
				.andExpect(status().isCreated());
		mockMvc.perform(get("/api/users")
					.with(user("admin@example.com").roles("ADMIN")))
				.andExpect(status().isOk());

		verify(categoryService).create(any());
		verify(userService).findAll();
	}

	@Test
	void inventoryManagerCanManageCatalogAndSuppliers() throws Exception {
		mockMvc.perform(post("/api/categories")
					.with(user("manager@example.com").roles("INVENTORY_MANAGER"))
					.contentType(MediaType.APPLICATION_JSON)
					.content(categoryJson()))
				.andExpect(status().isCreated());
		mockMvc.perform(post("/api/products")
					.with(user("manager@example.com").roles("INVENTORY_MANAGER"))
					.contentType(MediaType.APPLICATION_JSON)
					.content(productJson()))
				.andExpect(status().isCreated());
		mockMvc.perform(get("/api/suppliers")
					.with(user("manager@example.com").roles("INVENTORY_MANAGER")))
				.andExpect(status().isOk());
	}

	@Test
	void inventoryManagerCannotAdministerUsers() throws Exception {
		mockMvc.perform(get("/api/users")
					.with(user("manager@example.com").roles("INVENTORY_MANAGER")))
				.andExpect(status().isForbidden())
				.andExpect(jsonPath("$.status").value(403))
				.andExpect(jsonPath("$.message").value("Access denied"));
	}

	@Test
	void sellerCanReadProductsCategoriesAndCustomers() throws Exception {
		mockMvc.perform(get("/api/products").with(user("seller").roles("SELLER")))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/categories").with(user("seller").roles("SELLER")))
				.andExpect(status().isOk());
		mockMvc.perform(get("/api/customers").with(user("seller").roles("SELLER")))
				.andExpect(status().isOk());
	}

	@Test
	void sellerCanCreateCustomer() throws Exception {
		mockMvc.perform(post("/api/customers")
					.with(user("seller").roles("SELLER"))
					.contentType(MediaType.APPLICATION_JSON)
					.content(customerJson()))
				.andExpect(status().isCreated());
	}

	@Test
	void sellerCannotModifyProducts() throws Exception {
		mockMvc.perform(post("/api/products")
					.with(user("seller").roles("SELLER"))
					.contentType(MediaType.APPLICATION_JSON)
					.content(productJson()))
				.andExpect(status().isForbidden());
	}

	@Test
	void sellerCannotReadInventory() throws Exception {
		mockMvc.perform(get("/api/inventory").with(user("seller").roles("SELLER")))
				.andExpect(status().isForbidden());
	}

	@Test
	void validLoginIsPublicAndReturnsTokenWithoutPassword() throws Exception {
		UserResponse user = userResponse();
		when(authService.login(any(LoginRequest.class)))
				.thenReturn(new LoginResponse("signed.jwt", "Bearer", 3600L, user));

		mockMvc.perform(post("/api/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content(loginJson()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").value("signed.jwt"))
				.andExpect(jsonPath("$.tokenType").value("Bearer"))
				.andExpect(jsonPath("$..password").doesNotExist());
	}

	@Test
	void invalidLoginReturnsGeneric401() throws Exception {
		when(authService.login(any(LoginRequest.class)))
				.thenThrow(new BadCredentialsException("Invalid credentials"));

		mockMvc.perform(post("/api/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content(loginJson()))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.message").value("Invalid credentials"))
				.andExpect(jsonPath("$.status").value(401));
	}

	@Test
	void invalidLoginBodyReturns400WithoutAuthentication() throws Exception {
		mockMvc.perform(post("/api/auth/login")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"email\":\"not-an-email\",\"password\":\"\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.email").exists())
				.andExpect(jsonPath("$.fieldErrors.password").exists());
	}

	@Test
	void adminCanAccessAllOperationalApis() throws Exception {
		when(currentUserService.getCurrentUser()).thenReturn(domainUser());

		mockMvc.perform(post("/api/sales")
					.with(user("admin").roles("ADMIN"))
					.contentType(MediaType.APPLICATION_JSON)
					.content(saleJson()))
				.andExpect(status().isCreated());
		mockMvc.perform(post("/api/stock-entries")
					.with(user("admin").roles("ADMIN"))
					.contentType(MediaType.APPLICATION_JSON)
					.content(stockEntryJson()))
				.andExpect(status().isCreated());
		mockMvc.perform(post("/api/stock-exits")
					.with(user("admin").roles("ADMIN"))
					.contentType(MediaType.APPLICATION_JSON)
					.content(stockExitJson()))
				.andExpect(status().isCreated());
		mockMvc.perform(get("/api/inventory-movements")
					.with(user("admin").roles("ADMIN")))
				.andExpect(status().isOk());
	}

	@Test
	void inventoryManagerCanReadButCannotMutateSales() throws Exception {
		mockMvc.perform(get("/api/sales")
					.with(user("manager").roles("INVENTORY_MANAGER")))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/sales")
					.with(user("manager").roles("INVENTORY_MANAGER"))
					.contentType(MediaType.APPLICATION_JSON)
					.content(saleJson()))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/sales/1/complete")
					.with(user("manager").roles("INVENTORY_MANAGER")))
				.andExpect(status().isForbidden());
		mockMvc.perform(post("/api/sales/1/cancel")
					.with(user("manager").roles("INVENTORY_MANAGER")))
				.andExpect(status().isForbidden());
	}

	@Test
	void inventoryManagerCanManageStockEntriesExitsAndReadMovements() throws Exception {
		when(currentUserService.getCurrentUser()).thenReturn(domainUser());

		mockMvc.perform(post("/api/stock-entries")
					.with(user("manager").roles("INVENTORY_MANAGER"))
					.contentType(MediaType.APPLICATION_JSON)
					.content(stockEntryJson()))
				.andExpect(status().isCreated());
		mockMvc.perform(post("/api/stock-exits")
					.with(user("manager").roles("INVENTORY_MANAGER"))
					.contentType(MediaType.APPLICATION_JSON)
					.content(stockExitJson()))
				.andExpect(status().isCreated());
		mockMvc.perform(get("/api/inventory-movements")
					.with(user("manager").roles("INVENTORY_MANAGER")))
				.andExpect(status().isOk());
	}

	@Test
	void sellerCanReadCreateCompleteAndCancelSales() throws Exception {
		when(currentUserService.getCurrentUser()).thenReturn(domainUser());

		mockMvc.perform(get("/api/sales").with(user("seller").roles("SELLER")))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/sales")
					.with(user("seller").roles("SELLER"))
					.contentType(MediaType.APPLICATION_JSON)
					.content(saleJson()))
				.andExpect(status().isCreated());
		mockMvc.perform(post("/api/sales/1/complete")
					.with(user("seller").roles("SELLER")))
				.andExpect(status().isOk());
		mockMvc.perform(post("/api/sales/1/cancel")
					.with(user("seller").roles("SELLER")))
				.andExpect(status().isOk());
	}

	@Test
	void sellerCannotAccessStockEntriesExitsOrMovements() throws Exception {
		mockMvc.perform(get("/api/stock-entries").with(user("seller").roles("SELLER")))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/stock-exits").with(user("seller").roles("SELLER")))
				.andExpect(status().isForbidden());
		mockMvc.perform(get("/api/inventory-movements")
					.with(user("seller").roles("SELLER")))
				.andExpect(status().isForbidden());
	}

	@Test
	void allNewApisRequireAuthentication() throws Exception {
		mockMvc.perform(get("/api/sales")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/stock-entries")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/stock-exits")).andExpect(status().isUnauthorized());
		mockMvc.perform(get("/api/inventory-movements"))
				.andExpect(status().isUnauthorized());
	}

	private UserDetails principal(String authority) {
		return User.withUsername("user@example.com")
				.password("encoded")
				.authorities(new SimpleGrantedAuthority(authority))
				.build();
	}

	private com.stockflow.user.entity.User domainUser() {
		com.stockflow.user.entity.User user = new com.stockflow.user.entity.User();
		user.setId(7L);
		user.setActive(true);
		return user;
	}

	private UserResponse userResponse() {
		return new UserResponse(
				1L, "Admin", "User", "admin@example.com", true, Set.of(), null, null
		);
	}

	private String loginJson() {
		return "{\"email\":\"admin@example.com\",\"password\":\"secret123\"}";
	}

	private String categoryJson() {
		return "{\"name\":\"Hardware\",\"description\":\"Parts\",\"active\":true}";
	}

	private String productJson() {
		return "{\"sku\":\"SKU-1\",\"name\":\"Keyboard\","
				+ "\"description\":\"Mechanical\",\"salePrice\":99.90,"
				+ "\"active\":true,\"categoryId\":1}";
	}

	private String customerJson() {
		return "{\"documentNumber\":\"123\",\"firstName\":\"Ana\","
				+ "\"lastName\":\"Diaz\",\"email\":\"ana@example.com\","
				+ "\"phone\":\"555\",\"address\":\"Main St\",\"active\":true}";
	}

	private String saleJson() {
		return "{\"customerId\":null,\"items\":[{\"productId\":2,\"quantity\":1}]}";
	}

	private String stockEntryJson() {
		return "{\"supplierId\":2,\"referenceNumber\":\"REF-1\","
				+ "\"notes\":null,\"items\":[{\"productId\":3,"
				+ "\"quantity\":2,\"unitCost\":5.00}]}";
	}

	private String stockExitJson() {
		return "{\"reason\":\"DAMAGED\",\"notes\":null,"
				+ "\"items\":[{\"productId\":3,\"quantity\":2}]}";
	}
}
