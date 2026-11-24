package com.portfolio.hireflow.requisition;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.jayway.jsonpath.JsonPath;
import com.portfolio.hireflow.auth.repository.DepartmentRepository;
import com.portfolio.hireflow.requisition.repository.JobOpeningRepository;

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class RequisitionIntegrationTests {

    private static final String MANAGER_EMAIL = "manager@hireflow.local";
    private static final String HR_EMAIL = "hr@hireflow.local";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private JobOpeningRepository jobOpeningRepository;

    private MockMvc mockMvc;
    private Long engineeringId;
    private Long hrDepartmentId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
        engineeringId = departmentRepository.findByName("Engineering").orElseThrow().getId();
        hrDepartmentId = departmentRepository.findByName("Human Resources").orElseThrow().getId();
    }

    @Test
    void managerCanCreateAndReadOwnDepartmentDraft() throws Exception {
        Long id = createCompleteDraftAsManager();

        mockMvc.perform(get("/api/requisitions/{id}", id)
                        .with(user(MANAGER_EMAIL).roles("HIRING_MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DRAFT"))
                .andExpect(jsonPath("$.data.departmentName").value("Engineering"))
                .andExpect(jsonPath("$.data.canEdit").value(true))
                .andExpect(jsonPath("$.data.actions[0].actionType").value("CREATED"));
    }

    @Test
    void managerCannotCreateForAnotherDepartment() throws Exception {
        mockMvc.perform(post("/api/requisitions")
                        .with(user(MANAGER_EMAIL).roles("HIRING_MANAGER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(completeRequest(hrDepartmentId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void incompleteDraftCannotBeSubmitted() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/requisitions")
                        .with(user(MANAGER_EMAIL).roles("HIRING_MANAGER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"departmentId":%d,"jobTitle":"Backend Engineer"}
                                """.formatted(engineeringId)))
                .andExpect(status().isCreated())
                .andReturn();
        Long id = responseId(result);

        mockMvc.perform(post("/api/requisitions/{id}/submit", id)
                        .with(user(MANAGER_EMAIL).roles("HIRING_MANAGER"))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "送出前請完整填寫職稱、人數、原因、技能、薪資區間與希望到職日"));
    }

    @Test
    void approvalCreatesExactlyOneOpenJobOpening() throws Exception {
        Long id = createCompleteDraftAsManager();
        submitAsManager(id);

        mockMvc.perform(post("/api/requisitions/{id}/review", id)
                        .with(user(HR_EMAIL).roles("HR"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVE\",\"reason\":\"編制確認\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"))
                .andExpect(jsonPath("$.data.opening.status").value("OPEN"));

        mockMvc.perform(post("/api/requisitions/{id}/review", id)
                        .with(user(HR_EMAIL).roles("HR"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVE\"}"))
                .andExpect(status().isBadRequest());

        org.junit.jupiter.api.Assertions.assertEquals(1, jobOpeningRepository.countByRequisitionId(id));
    }

    @Test
    void returnRequiresReasonAndAllowsCreatorToResubmit() throws Exception {
        Long id = createCompleteDraftAsManager();
        submitAsManager(id);

        mockMvc.perform(post("/api/requisitions/{id}/review", id)
                        .with(user(HR_EMAIL).roles("HR"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"RETURN\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("退回理由為必填"));

        mockMvc.perform(post("/api/requisitions/{id}/review", id)
                        .with(user(HR_EMAIL).roles("HR"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"RETURN\",\"reason\":\"請補充技能細節\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RETURNED"))
                .andExpect(jsonPath("$.data.canEdit").value(true));

        submitAsManager(id);
    }

    @Test
    void managerListIsRestrictedToOwnDepartment() throws Exception {
        mockMvc.perform(post("/api/requisitions")
                        .with(user(HR_EMAIL).roles("HR"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(completeRequest(hrDepartmentId)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/requisitions")
                        .param("departmentId", hrDepartmentId.toString())
                        .with(user(MANAGER_EMAIL).roles("HIRING_MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0));
    }

    @Test
    void interviewerCannotAccessRequisitions() throws Exception {
        mockMvc.perform(get("/api/requisitions")
                        .with(user("interviewer@hireflow.local").roles("INTERVIEWER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void staleVersionUpdateReturnsConflict() throws Exception {
        Long id = createCompleteDraftAsManager();

        mockMvc.perform(put("/api/requisitions/{id}", id)
                        .with(user(MANAGER_EMAIL).roles("HIRING_MANAGER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "version":99,
                                  "departmentId":%d,
                                  "jobTitle":"Backend Engineer",
                                  "headcount":2,
                                  "reason":"產品擴編",
                                  "requiredSkills":"Java, Spring Boot",
                                  "salaryMin":60000,
                                  "salaryMax":90000,
                                  "targetStartDate":"2027-01-15"
                                }
                                """.formatted(engineeringId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("資料已被更新，請重新整理後再編輯"));
    }

    private Long createCompleteDraftAsManager() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/requisitions")
                        .with(user(MANAGER_EMAIL).roles("HIRING_MANAGER"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(completeRequest(engineeringId)))
                .andExpect(status().isCreated())
                .andReturn();
        return responseId(result);
    }

    private void submitAsManager(Long id) throws Exception {
        mockMvc.perform(post("/api/requisitions/{id}/submit", id)
                        .with(user(MANAGER_EMAIL).roles("HIRING_MANAGER"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"));
    }

    private Long responseId(MvcResult result) throws Exception {
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");
        return id.longValue();
    }

    private String completeRequest(Long departmentId) {
        return """
                {
                  "departmentId":%d,
                  "jobTitle":"Backend Engineer",
                  "headcount":2,
                  "reason":"產品擴編",
                  "requiredSkills":"Java, Spring Boot",
                  "salaryMin":60000,
                  "salaryMax":90000,
                  "targetStartDate":"2027-01-15"
                }
                """.formatted(departmentId);
    }
}
