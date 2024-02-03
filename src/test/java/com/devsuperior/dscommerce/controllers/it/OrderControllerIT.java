package com.devsuperior.dscommerce.controllers.it;

import com.devsuperior.dscommerce.dto.OrderDTO;
import com.devsuperior.dscommerce.entities.*;
import com.devsuperior.dscommerce.tests.ProductFactory;
import com.devsuperior.dscommerce.tests.TokenUtil;
import com.devsuperior.dscommerce.tests.UserFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class OrderControllerIT {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private TokenUtil tokenUtil;
    @Autowired
    private ObjectMapper objectMapper;

    private String clientUsername, clientPassword, adminUsername, adminPassword, adminOnlyUsername, adminOnlyPassword;
    private String clientToken, adminToken, invalidToken, adminOnlyToken;
    private Long existingOrderId, nonExistingOrderId;
    private Order order;
    private OrderDTO orderDTO;
    private User user;

    @BeforeEach
    void setUp() throws Exception {

        clientUsername = "maria@gmail.com";
        clientPassword = "123456";
        adminUsername = "alex@gmail.com";
        adminPassword = "123456";
        adminOnlyUsername = "ana@gmail.com";
        adminOnlyPassword = "123456";

        existingOrderId = 1L;
        nonExistingOrderId = 1000L;

        adminToken = tokenUtil.obtainAccessToken(mockMvc, adminUsername, adminPassword);
        clientToken = tokenUtil.obtainAccessToken(mockMvc, clientUsername, clientPassword);
        adminOnlyToken = tokenUtil.obtainAccessToken(mockMvc, adminOnlyUsername, adminOnlyPassword);
        invalidToken = adminToken + "xxxx"; //simulates wrong password

        user = UserFactory.createClientUser();
        order = new Order(null, Instant.now(), OrderStatus.WAITING_PAYMENT, user, null);

        Product product = ProductFactory.createProducts();
        OrderItem orderItem = new OrderItem(order, product, 2, 10.0);
        order.getItems().add(orderItem);

        orderDTO = new OrderDTO(order);


    }

    @Test
    public void findByIdShouldReturnOrderDTOWhenIdExistsAdminLogged() throws Exception {

        ResultActions resultActions = mockMvc
                .perform(get("/orders/{id}", existingOrderId)
                        .header("Authorization", "Bearer " + adminToken)
                        .accept(MediaType.APPLICATION_JSON));


        resultActions.andExpect(status().isOk());
        resultActions.andExpect(jsonPath("$.id").value(existingOrderId));
        resultActions.andExpect(jsonPath("$.moment").value("2022-07-25T13:00:00Z"));
        resultActions.andExpect(jsonPath("$.status").value("PAID"));
        resultActions.andExpect(jsonPath("$.client").exists());
        resultActions.andExpect(jsonPath("$.client.name").value("Maria Brown"));
        resultActions.andExpect(jsonPath("$.payment").exists());
        resultActions.andExpect(jsonPath("$.items").exists());
        resultActions.andExpect(jsonPath("$.items[1].name").value("Macbook Pro"));
        resultActions.andExpect(jsonPath("$.total").exists());

    }

    @Test
    public void findByIdShouldReturnOrderDTOWhenIdExistsClientLogged() throws Exception {

        ResultActions resultActions = mockMvc
                .perform(get("/orders/{id}", existingOrderId)
                        .header("Authorization", "Bearer " + clientToken)
                        .accept(MediaType.APPLICATION_JSON));


        resultActions.andExpect(status().isOk());
        resultActions.andExpect(jsonPath("$.id").value(existingOrderId));
        resultActions.andExpect(jsonPath("$.moment").value("2022-07-25T13:00:00Z"));
        resultActions.andExpect(jsonPath("$.status").value("PAID"));
        resultActions.andExpect(jsonPath("$.client").exists());
        resultActions.andExpect(jsonPath("$.client.name").value("Maria Brown"));
        resultActions.andExpect(jsonPath("$.payment").exists());
        resultActions.andExpect(jsonPath("$.items").exists());
        resultActions.andExpect(jsonPath("$.items[1].name").value("Macbook Pro"));
        resultActions.andExpect(jsonPath("$.total").exists());

    }

    @Test
    public void findByIdShouldReturnForbiddenWhenIdExistsClientLoggedAndOrderDoesNotBelongUser() throws Exception {

        Long otherOrderId = 2L;
        ResultActions resultActions = mockMvc
                .perform(get("/orders/{id}", otherOrderId)
                        .header("Authorization", "Bearer " + clientToken)
                        .accept(MediaType.APPLICATION_JSON));

        resultActions.andExpect(status().isForbidden());

    }

    @Test
    public void findByIdShouldReturnNotFoundWhenDoesNotExistsAdminLogged() throws Exception {

        ResultActions resultActions = mockMvc
                .perform(get("/orders/{id}", nonExistingOrderId)
                        .header("Authorization", "Bearer " + adminToken)
                        .accept(MediaType.APPLICATION_JSON));

        resultActions.andExpect(status().isNotFound());

    }

    @Test
    public void findByIdShouldReturnNotFoundWhenDoesNotExistsClientLogged() throws Exception {

        ResultActions resultActions = mockMvc
                .perform(get("/orders/{id}", nonExistingOrderId)
                        .header("Authorization", "Bearer " + clientToken)
                        .accept(MediaType.APPLICATION_JSON));

        resultActions.andExpect(status().isNotFound());

    }

    @Test
    public void findByIdShouldReturnUnathorizedWhenExistsAndInvalidToken() throws Exception {

        ResultActions resultActions = mockMvc
                .perform(get("/orders/{id}", existingOrderId)
                        .header("Authorization", "Bearer " + invalidToken)
                        .accept(MediaType.APPLICATION_JSON));

        resultActions.andExpect(status().isUnauthorized());

    }

    @Test
    public void insertShouldReturnOrderDTOCreatedWhenClientLogged() throws Exception {

        String jsonBody = objectMapper.writeValueAsString(orderDTO);

        ResultActions result =
                mockMvc.perform(post("/orders")
                                .header("Authorization", "Bearer " + clientToken)
                                .content(jsonBody)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON))
                        .andDo(MockMvcResultHandlers.print());

        result.andExpect(status().isCreated());
        result.andExpect(jsonPath("$.id").value(4L));
        result.andExpect(jsonPath("$.moment").exists());
        result.andExpect(jsonPath("$.status").value("WAITING_PAYMENT"));
        result.andExpect(jsonPath("$.client").exists());
        result.andExpect(jsonPath("$.items").exists());
        result.andExpect(jsonPath("$.total").exists());
    }

    @Test
    public void insertShouldReturnUnprocessableEntityWhenClientLoggedAndOrderHasNoItem() throws Exception {

        orderDTO.getItems().clear();

        String jsonBody = objectMapper.writeValueAsString(orderDTO);

        ResultActions result =
                mockMvc.perform(post("/orders")
                                .header("Authorization", "Bearer " + clientToken)
                                .content(jsonBody)
                                .contentType(MediaType.APPLICATION_JSON)
                                .accept(MediaType.APPLICATION_JSON))
                        .andDo(MockMvcResultHandlers.print());

        result.andExpect(status().isUnprocessableEntity());
    }

    @Test
    public void insertShouldReturnForbiddenWhenAdminLogged() throws Exception {

        String jsonBody = objectMapper.writeValueAsString(orderDTO);

        ResultActions result =
                mockMvc.perform(post("/orders")
                        .header("Authorization", "Bearer " + adminOnlyToken)
                        .content(jsonBody)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isForbidden());
    }
    @Test
    public void insertShouldReturnUnauthorizedWhenInvalidToken() throws Exception {

        String jsonBody = objectMapper.writeValueAsString(orderDTO);

        ResultActions result =
                mockMvc.perform(post("/orders")
                        .header("Authorization", "Bearer " + invalidToken)
                        .content(jsonBody)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON));

        result.andExpect(status().isUnauthorized());
    }

}
