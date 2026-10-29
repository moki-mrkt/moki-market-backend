package ua.moki.modules.products.controllers;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ua.moki.configuration.AppConfig;
import ua.moki.configuration.JwtCryptoConfig;
import ua.moki.configuration.SecurityConfig;
import ua.moki.configuration.entry_points.JwtAuthenticationEntryPoint;
import ua.moki.modules.products.dtos.ProductImportErrorDTO;
import ua.moki.modules.products.dtos.ProductImportResultDTO;
import ua.moki.modules.products.services.ProductImportService;
import ua.moki.modules.users.security.JwtFilter;

import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductImportController.class)
@Import({SecurityConfig.class, JwtCryptoConfig.class, AppConfig.class,
        JwtAuthenticationEntryPoint.class})
class ProductImportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductImportService productImportService;

    @MockitoBean
    private JwtFilter jwtFilter;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMetamodelMappingContext;

    @BeforeEach
    void setup() throws Exception {
        doAnswer(invocation -> {
            HttpServletRequest req = invocation.getArgument(0);
            HttpServletResponse res = invocation.getArgument(1);
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(req, res);
            return null;
        }).when(jwtFilter).doFilter(any(), any(), any());
    }

    @Test
    @DisplayName("POST /import - 200 OK for ADMIN with import report")
    void importProducts_shouldReturnOk_whenAdminUploadsExcel() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "products.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[]{1, 2, 3, 4}
        );

        ProductImportResultDTO resultDTO = new ProductImportResultDTO(
                3,
                2,
                1,
                List.of(new ProductImportErrorDTO(4, "Товар з помилкою", "Ціна має бути більше 0"))
        );

        when(productImportService.importProductsFromExcel(any())).thenReturn(resultDTO);

        mockMvc.perform(multipart("/import")
                        .file(file)
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRows", is(3)))
                .andExpect(jsonPath("$.importedCount", is(2)))
                .andExpect(jsonPath("$.skippedCount", is(1)))
                .andExpect(jsonPath("$.errors", hasSize(1)))
                .andExpect(jsonPath("$.errors[0].rowNumber", is(4)));

        verify(productImportService).importProductsFromExcel(any());
    }

    @Test
    @DisplayName("POST /import - 200 OK for MANAGER")
    void importProducts_shouldReturnOk_whenManagerUploadsExcel() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "products.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[]{1, 2, 3}
        );

        ProductImportResultDTO resultDTO = new ProductImportResultDTO(1, 1, 0, List.of());
        when(productImportService.importProductsFromExcel(any())).thenReturn(resultDTO);

        mockMvc.perform(multipart("/import")
                        .file(file)
                        .with(user("manager").roles("MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.importedCount", is(1)));

        verify(productImportService).importProductsFromExcel(any());
    }

    @Test
    @DisplayName("POST /import - 403 Forbidden for CUSTOMER")
    void importProducts_shouldReturnForbidden_whenCustomer() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "products.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[]{1, 2, 3}
        );

        mockMvc.perform(multipart("/import")
                        .file(file)
                        .with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isForbidden());

        verify(productImportService, never()).importProductsFromExcel(any());
    }

    @Test
    @DisplayName("POST /import - 401 Unauthorized for anonymous user")
    void importProducts_shouldReturnUnauthorized_whenAnonymous() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "products.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[]{1, 2, 3}
        );

        mockMvc.perform(multipart("/import")
                        .file(file))
                .andExpect(status().isUnauthorized());

        verify(productImportService, never()).importProductsFromExcel(any());
    }

    @Test
    @DisplayName("GET /import/template - 200 OK with xlsx file for ADMIN")
    void downloadTemplate_shouldReturnExcelBytes_whenAdmin() throws Exception {
        byte[] templateBytes = new byte[]{0x50, 0x4B, 0x03, 0x04};
        when(productImportService.generateTemplate()).thenReturn(templateBytes);

        mockMvc.perform(get("/import/template")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("moki-products-import-template.xlsx")))
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(content().bytes(templateBytes));

        verify(productImportService).generateTemplate();
    }

    @Test
    @DisplayName("GET /import/template - 403 Forbidden for CUSTOMER")
    void downloadTemplate_shouldReturnForbidden_whenCustomer() throws Exception {
        mockMvc.perform(get("/import/template")
                        .with(user("customer").roles("CUSTOMER")))
                .andExpect(status().isForbidden());

        verify(productImportService, never()).generateTemplate();
    }
}
