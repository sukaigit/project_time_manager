package com.timemanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.timemanager.config.JwtUtil;
import com.timemanager.dto.*;
import com.timemanager.entity.*;
import com.timemanager.mapper.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive Spring Boot controller integration tests.
 * Uses MockMvc with real JWT tokens and real MySQL database.
 * Each test rolls back via @Transactional.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AllControllersIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private BCryptPasswordEncoder encoder;

    @Autowired private UserMapper userMapper;
    @Autowired private ProjectMapper projectMapper;
    @Autowired private TaskModuleMapper moduleMapper;
    @Autowired private WorkHourMapper workHourMapper;
    @Autowired private ApprovalMapper approvalMapper;
    @Autowired private OperationLogMapper operationLogMapper;

    // Test data IDs
    private Long adminId;
    private Long pmId;
    private Long userId;
    private Long deptMgrId;
    private Long projectId;
    private Long moduleId;
    private Long workHourId;
    private Long approvalId;
    private Long logId;

    // JWT tokens for different roles
    private String adminToken;
    private String pmToken;
    private String userToken;
    private String deptMgrToken;

    private static final String ADMIN_USERNAME = "ctlr_admin";
    private static final String PM_USERNAME = "ctlr_pm";
    private static final String USER_USERNAME = "ctlr_user";
    private static final String DEPT_MGR_USERNAME = "ctlr_dept_mgr";
    private static final String PASSWORD_OR = "123456";

    // ========== Utility Methods ==========

    private String toJson(Object obj) throws Exception {
        return objectMapper.writeValueAsString(obj);
    }

    private ResultActions performGet(String url, String token) throws Exception {
        var req = get(url).contentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            req = req.header("Authorization", "Bearer " + token);
        }
        return mockMvc.perform(req);
    }

    private ResultActions performPost(String url, Object body, String token) throws Exception {
        var req = post(url).contentType(MediaType.APPLICATION_JSON).content(toJson(body));
        if (token != null) {
            req = req.header("Authorization", "Bearer " + token);
        }
        return mockMvc.perform(req);
    }

    private ResultActions performPut(String url, Object body, String token) throws Exception {
        var req = put(url).contentType(MediaType.APPLICATION_JSON).content(toJson(body));
        if (token != null) {
            req = req.header("Authorization", "Bearer " + token);
        }
        return mockMvc.perform(req);
    }

    private ResultActions performDelete(String url, String token) throws Exception {
        var req = delete(url).contentType(MediaType.APPLICATION_JSON);
        if (token != null) {
            req = req.header("Authorization", "Bearer " + token);
        }
        return mockMvc.perform(req);
    }

    private void assertSuccess(ResultActions result) throws Exception {
        result.andExpect(status().isOk())
              .andExpect(jsonPath("$.code").value(200));
    }

    private void assertBizError(ResultActions result, String expectedMsg) throws Exception {
        result.andExpect(status().isOk())
              .andExpect(jsonPath("$.code").value(400))
              .andExpect(jsonPath("$.message").value(expectedMsg));
    }

    private void assertForbidden(ResultActions result) throws Exception {
        result.andExpect(status().isForbidden());
    }

    // ========== Test Data Setup ==========

    @BeforeEach
    void setUp() {
        // Admin
        User admin = new User();
        admin.setUsername(ADMIN_USERNAME);
        admin.setPassword(encoder.encode(PASSWORD_OR));
        admin.setName("管理员");
        admin.setEmail("admin@test.com");
        admin.setPhone("13800000001");
        admin.setDepartment("研发与交付中心");
        admin.setRole("ADMIN");
        admin.setStatus(1);
        admin.setFirstLogin(0);
        userMapper.insert(admin);
        adminId = admin.getId();
        adminToken = jwtUtil.generateToken(adminId, ADMIN_USERNAME, "ADMIN");

        // PM
        User pm = new User();
        pm.setUsername(PM_USERNAME);
        pm.setPassword(encoder.encode(PASSWORD_OR));
        pm.setName("项目经理");
        pm.setEmail("pm@test.com");
        pm.setPhone("13800000002");
        pm.setDepartment("研发与交付中心");
        pm.setRole("PM");
        pm.setStatus(1);
        pm.setFirstLogin(0);
        userMapper.insert(pm);
        pmId = pm.getId();
        pmToken = jwtUtil.generateToken(pmId, PM_USERNAME, "PM");

        // Regular user
        User user = new User();
        user.setUsername(USER_USERNAME);
        user.setPassword(encoder.encode(PASSWORD_OR));
        user.setName("普通用户");
        user.setEmail("user@test.com");
        user.setPhone("13800000003");
        user.setDepartment("研发与交付中心");
        user.setRole("USER");
        user.setStatus(1);
        user.setFirstLogin(1);
        userMapper.insert(user);
        userId = user.getId();
        userToken = jwtUtil.generateToken(userId, USER_USERNAME, "USER");

        // Dept manager
        User deptMgr = new User();
        deptMgr.setUsername(DEPT_MGR_USERNAME);
        deptMgr.setPassword(encoder.encode(PASSWORD_OR));
        deptMgr.setName("部门经理");
        deptMgr.setEmail("deptmgr@test.com");
        deptMgr.setPhone("13800000004");
        deptMgr.setDepartment("研发与交付中心");
        deptMgr.setRole("DEPT_MANAGER");
        deptMgr.setStatus(1);
        deptMgr.setFirstLogin(0);
        userMapper.insert(deptMgr);
        deptMgrId = deptMgr.getId();
        deptMgrToken = jwtUtil.generateToken(deptMgrId, DEPT_MGR_USERNAME, "DEPT_MANAGER");

        // Common data: Project
        Project p = new Project();
        p.setName("集成测试项目");
        p.setDescription("用于集成测试");
        p.setManagerId(pmId);
        p.setStartDate(LocalDate.of(2026, 1, 1));
        p.setEndDate(LocalDate.of(2026, 12, 31));
        p.setStatus("ACTIVE");
        projectMapper.insert(p);
        projectId = p.getId();

        // Common data: Module
        TaskModule m = new TaskModule();
        m.setProjectId(projectId);
        m.setName("集成测试模块");
        m.setDescription("用于集成测试");
        m.setEstimatedHours(100);
        moduleMapper.insert(m);
        moduleId = m.getId();

        // Common data: WorkHour (by user)
        WorkHour wh = new WorkHour();
        wh.setUserId(userId);
        wh.setProjectId(projectId);
        wh.setModuleId(moduleId);
        wh.setWorkDate(LocalDate.of(2026, 6, 1));
        wh.setHours(new BigDecimal("8.0"));
        wh.setContent("测试工时");
        wh.setStatus("PENDING");
        workHourMapper.insert(wh);
        workHourId = wh.getId();

        // Common data: Approval
        Approval ap = new Approval();
        ap.setWorkHourId(workHourId);
        ap.setApproverId(pmId);
        ap.setStatus("APPROVED");
        ap.setComment("审批通过");
        ap.setApproveTime(LocalDateTime.now());
        approvalMapper.insert(ap);
        approvalId = ap.getId();

        // Common data: OperationLog
        OperationLog ol = new OperationLog();
        ol.setUserId(adminId);
        ol.setAction("LOGIN");
        ol.setTarget("用户登录");
        ol.setDetail("管理员登录系统");
        ol.setCreateTime(LocalDateTime.now());
        operationLogMapper.insert(ol);
        logId = ol.getId();
    }

    // =============================================================
    //  AUTH CONTROLLER (9 endpoints: login, captcha, changePassword)
    // =============================================================

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class AuthControllerTests {

        @Test
        @Order(1)
        void testLogin_success_debugSkip() throws Exception {
            LoginRequest req = new LoginRequest();
            req.setUsername(ADMIN_USERNAME);
            req.setPassword(PASSWORD_OR);
            req.setCaptchaId("dummy");
            req.setCaptchaCode("debug_skip");

            var result = performPost("/api/auth/login", req, null);
            assertSuccess(result);
            result.andExpect(jsonPath("$.data.token").isString())
                  .andExpect(jsonPath("$.data.userId").value(adminId))
                  .andExpect(jsonPath("$.data.username").value(ADMIN_USERNAME))
                  .andExpect(jsonPath("$.data.name").value("管理员"))
                  .andExpect(jsonPath("$.data.role").value("ADMIN"))
                  .andExpect(jsonPath("$.data.firstLogin").value(false));
        }

        @Test
        @Order(2)
        void testLogin_wrongCaptcha() throws Exception {
            LoginRequest req = new LoginRequest();
            req.setUsername(ADMIN_USERNAME);
            req.setPassword(PASSWORD_OR);
            req.setCaptchaId("nonexistent_captcha_id");
            req.setCaptchaCode("wrong_code");

            var result = performPost("/api/auth/login", req, null);
            assertBizError(result, "验证码错误");
        }

        @Test
        @Order(3)
        void testLogin_wrongPassword() throws Exception {
            LoginRequest req = new LoginRequest();
            req.setUsername(ADMIN_USERNAME);
            req.setPassword("wrong_password");
            req.setCaptchaId("dummy");
            req.setCaptchaCode("debug_skip");

            var result = performPost("/api/auth/login", req, null);
            assertBizError(result, "用户名或密码错误");
        }

        @Test
        @Order(4)
        void testLogin_disabledUser() throws Exception {
            User disabled = new User();
            disabled.setUsername("disabled_user_ctlr");
            disabled.setPassword(encoder.encode(PASSWORD_OR));
            disabled.setName("已禁用");
            disabled.setRole("USER");
            disabled.setStatus(0);
            disabled.setFirstLogin(0);
            userMapper.insert(disabled);

            LoginRequest req = new LoginRequest();
            req.setUsername("disabled_user_ctlr");
            req.setPassword(PASSWORD_OR);
            req.setCaptchaId("dummy");
            req.setCaptchaCode("debug_skip");

            var result = performPost("/api/auth/login", req, null);
            assertBizError(result, "账户已被禁用");
        }

        @Test
        @Order(5)
        void testLogin_userNotFound() throws Exception {
            LoginRequest req = new LoginRequest();
            req.setUsername("nonexistent_user");
            req.setPassword(PASSWORD_OR);
            req.setCaptchaId("dummy");
            req.setCaptchaCode("debug_skip");

            var result = performPost("/api/auth/login", req, null);
            assertBizError(result, "用户名或密码错误");
        }

        @Test
        @Order(6)
        void testLogin_firstLogin() throws Exception {
            LoginRequest req = new LoginRequest();
            req.setUsername(USER_USERNAME);
            req.setPassword(PASSWORD_OR);
            req.setCaptchaId("dummy");
            req.setCaptchaCode("debug_skip");

            var result = performPost("/api/auth/login", req, null);
            assertSuccess(result);
            result.andExpect(jsonPath("$.data.firstLogin").value(true));
        }

        @Test
        @Order(7)
        void testCaptcha() throws Exception {
            mockMvc.perform(get("/api/auth/captcha"))
                   .andExpect(status().isOk())
                   .andExpect(header().exists("X-Captcha-Id"))
                   .andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_PNG));
        }

        @Test
        @Order(8)
        void testChangePassword_success() throws Exception {
            PasswordRequest req = new PasswordRequest();
            req.setOldPassword(PASSWORD_OR);
            req.setNewPassword("newpass123");

            var result = performPut("/api/auth/password", req, userToken);
            assertSuccess(result);
        }

        @Test
        @Order(9)
        void testChangePassword_wrongOldPassword() throws Exception {
            PasswordRequest req = new PasswordRequest();
            req.setOldPassword("wrong_old");
            req.setNewPassword("newpass123");

            var result = performPut("/api/auth/password", req, userToken);
            assertBizError(result, "旧密码错误");
        }

        @Test
        @Order(10)
        void testChangePassword_withoutAuth() throws Exception {
            // /api/auth/** is permitAll, so this returns 200 with code 400 "用户不存在"
            PasswordRequest req = new PasswordRequest();
            req.setOldPassword(PASSWORD_OR);
            req.setNewPassword("newpass123");

            var result = performPut("/api/auth/password", req, null);
            assertBizError(result, "用户不存在");
        }

        @Test
        @Order(11)
        void testChangePassword_userNotFound() throws Exception {
            // Use a token for a non-existent user
            String ghostToken = jwtUtil.generateToken(99999L, "ghost_user", "USER");
            PasswordRequest req = new PasswordRequest();
            req.setOldPassword(PASSWORD_OR);
            req.setNewPassword("newpass123");

            var result = performPut("/api/auth/password", req, ghostToken);
            assertBizError(result, "用户不存在");
        }
    }

    // =============================================================
    //  USER CONTROLLER (7 endpoints: list, pm-list, create, update, delete, updateRole, resetPassword)
    // =============================================================

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class UserControllerTests {

        @Test
        @Order(1)
        void testListUsers_asAdmin() throws Exception {
            var result = performGet("/api/users?page=1&size=10", adminToken);
            assertSuccess(result);
            result.andExpect(jsonPath("$.data.total").isNumber())
                  .andExpect(jsonPath("$.data.list").isArray());
        }

        @Test
        @Order(2)
        void testListUsers_asUser_forbidden() throws Exception {
            var result = performGet("/api/users?page=1&size=10", userToken);
            assertForbidden(result);
        }

        @Test
        @Order(3)
        void testListUsers_withKeyword() throws Exception {
            var result = performGet("/api/users?page=1&size=10&keyword=管理员", adminToken);
            assertSuccess(result);
            result.andExpect(jsonPath("$.data.list[0].name").value("管理员"));
        }

        @Test
        @Order(4)
        void testListPms_asAdmin() throws Exception {
            var result = performGet("/api/users/pm-list", adminToken);
            assertSuccess(result);
            result.andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @Order(5)
        void testListPms_asPm() throws Exception {
            var result = performGet("/api/users/pm-list", pmToken);
            assertSuccess(result);
            result.andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @Order(6)
        void testListPms_asUser_forbidden() throws Exception {
            var result = performGet("/api/users/pm-list", userToken);
            assertForbidden(result);
        }

        @Test
        @Order(7)
        void testCreateUser_success() throws Exception {
            UserRequest req = new UserRequest();
            req.setUsername("new_ctlr_user");
            req.setName("新用户");
            req.setEmail("new@test.com");
            req.setDepartment("测试部");
            req.setRole("USER");
            req.setStatus(1);

            var result = performPost("/api/users", req, adminToken);
            assertSuccess(result);
            result.andExpect(jsonPath("$.data.id").isNumber());
        }

        @Test
        @Order(8)
        void testCreateUser_missingName() throws Exception {
            UserRequest req = new UserRequest();
            req.setUsername("noname_user");

            var result = performPost("/api/users", req, adminToken);
            assertBizError(result, "姓名不能为空");
        }

        @Test
        @Order(9)
        void testCreateUser_missingUsername() throws Exception {
            UserRequest req = new UserRequest();
            req.setUsername("");
            req.setName("有姓无名");

            var result = performPost("/api/users", req, adminToken);
            assertBizError(result, "用户名不能为空");
        }

        @Test
        @Order(10)
        void testCreateUser_duplicateUsername() throws Exception {
            UserRequest req = new UserRequest();
            req.setUsername(ADMIN_USERNAME);
            req.setName("重名用户");

            var result = performPost("/api/users", req, adminToken);
            assertBizError(result, "用户名已存在");
        }

        @Test
        @Order(11)
        void testUpdateUser_success() throws Exception {
            UserRequest req = new UserRequest();
            req.setName("更新的名字");
            req.setEmail("updated@test.com");

            var result = performPut("/api/users/" + userId, req, adminToken);
            assertSuccess(result);
        }

        @Test
        @Order(12)
        void testUpdateUser_notFound() throws Exception {
            UserRequest req = new UserRequest();
            req.setName("不存在");

            var result = performPut("/api/users/99999", req, adminToken);
            assertBizError(result, "用户不存在");
        }

        @Test
        @Order(13)
        void testDeleteUser_success() throws Exception {
            User disposable = new User();
            disposable.setUsername("delete_me_ctlr");
            disposable.setPassword(encoder.encode(PASSWORD_OR));
            disposable.setName("待删除");
            disposable.setRole("USER");
            disposable.setStatus(1);
            disposable.setFirstLogin(0);
            userMapper.insert(disposable);

            var result = performDelete("/api/users/" + disposable.getId(), adminToken);
            assertSuccess(result);
        }

        @Test
        @Order(14)
        void testDeleteUser_notFound() throws Exception {
            var result = performDelete("/api/users/99999", adminToken);
            assertBizError(result, "用户不存在");
        }

        @Test
        @Order(15)
        void testUpdateRole_success() throws Exception {
            RoleRequest req = new RoleRequest();
            req.setRole("PM");

            var result = performPut("/api/users/" + userId + "/role", req, adminToken);
            assertSuccess(result);
        }

        @Test
        @Order(16)
        void testUpdateRole_emptyRole() throws Exception {
            RoleRequest req = new RoleRequest();
            req.setRole("");

            var result = performPut("/api/users/" + userId + "/role", req, adminToken);
            assertBizError(result, "角色不能为空");
        }

        @Test
        @Order(17)
        void testUpdateRole_notFound() throws Exception {
            RoleRequest req = new RoleRequest();
            req.setRole("PM");

            var result = performPut("/api/users/99999/role", req, adminToken);
            assertBizError(result, "用户不存在");
        }

        @Test
        @Order(18)
        void testResetPassword_success() throws Exception {
            var result = performPut("/api/users/" + userId + "/reset-password", null, adminToken);
            assertSuccess(result);
        }

        @Test
        @Order(19)
        void testResetPassword_notFound() throws Exception {
            var result = performPut("/api/users/99999/reset-password", null, adminToken);
            assertBizError(result, "用户不存在");
        }

        @Test
        @Order(20)
        void testUserCreate_asUser_forbidden() throws Exception {
            UserRequest req = new UserRequest();
            req.setUsername("should_fail");
            req.setName("应失败");

            var result = performPost("/api/users", req, userToken);
            assertForbidden(result);
        }
    }

    // =============================================================
    //  PROJECT CONTROLLER (4 endpoints: list, create, update, delete)
    // =============================================================

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class ProjectControllerTests {

        @Test
        @Order(1)
        void testListProjects_asAdmin() throws Exception {
            var result = performGet("/api/projects?page=1&size=10", adminToken);
            assertSuccess(result);
            result.andExpect(jsonPath("$.data.total").isNumber())
                  .andExpect(jsonPath("$.data.list").isArray());
        }

        @Test
        @Order(2)
        void testListProjects_asUser_forbidden() throws Exception {
            var result = performGet("/api/projects?page=1&size=10", userToken);
            assertForbidden(result);
        }

        @Test
        @Order(3)
        void testListProjects_withKeyword() throws Exception {
            var result = performGet("/api/projects?page=1&size=10&keyword=集成测试", adminToken);
            assertSuccess(result);
            result.andExpect(jsonPath("$.data.list").isArray());
        }

        @Test
        @Order(4)
        void testCreateProject_success() throws Exception {
            ProjectRequest req = new ProjectRequest();
            req.setName("新项目");
            req.setDescription("新项目描述");
            req.setManagerId(pmId);
            req.setStartDate("2026-06-01");
            req.setEndDate("2026-12-31");

            var result = performPost("/api/projects", req, adminToken);
            assertSuccess(result);
            result.andExpect(jsonPath("$.data.id").isNumber());
        }

        @Test
        @Order(5)
        void testCreateProject_missingName() throws Exception {
            ProjectRequest req = new ProjectRequest();
            req.setManagerId(pmId);

            var result = performPost("/api/projects", req, adminToken);
            assertBizError(result, "项目名称不能为空");
        }

        @Test
        @Order(6)
        void testCreateProject_missingManagerId() throws Exception {
            ProjectRequest req = new ProjectRequest();
            req.setName("无经理项目");

            var result = performPost("/api/projects", req, adminToken);
            assertBizError(result, "项目经理不能为空");
        }

        @Test
        @Order(7)
        void testUpdateProject_success() throws Exception {
            ProjectRequest req = new ProjectRequest();
            req.setName("更新的项目");
            req.setManagerId(pmId);

            var result = performPut("/api/projects/" + projectId, req, adminToken);
            assertSuccess(result);
        }

        @Test
        @Order(8)
        void testDeleteProject_success() throws Exception {
            var result = performDelete("/api/projects/" + projectId, adminToken);
            assertSuccess(result);
        }

        @Test
        @Order(9)
        void testCreateProject_asUser_forbidden() throws Exception {
            ProjectRequest req = new ProjectRequest();
            req.setName("不应创建");
            req.setManagerId(pmId);

            var result = performPost("/api/projects", req, userToken);
            assertForbidden(result);
        }

        @Test
        @Order(10)
        void testProjectEndpoints_withoutAuth() throws Exception {
            performGet("/api/projects?page=1&size=10", null).andExpect(status().isForbidden());
            performPost("/api/projects", new ProjectRequest(), null).andExpect(status().isForbidden());
        }
    }

    // =============================================================
    //  MODULE CONTROLLER (4 endpoints: list, create, update, delete)
    // =============================================================

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class ModuleControllerTests {

        @Test
        @Order(1)
        void testListModules_success() throws Exception {
            var result = performGet("/api/projects/" + projectId + "/modules", adminToken);
            assertSuccess(result);
            result.andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @Order(2)
        void testCreateModule_success() throws Exception {
            ModuleRequest req = new ModuleRequest();
            req.setName("新模块");
            req.setDescription("新模块描述");
            req.setEstimatedHours(50);

            var result = performPost("/api/projects/" + projectId + "/modules", req, adminToken);
            assertSuccess(result);
            result.andExpect(jsonPath("$.data.id").isNumber());
        }

        @Test
        @Order(3)
        void testCreateModule_missingName() throws Exception {
            ModuleRequest req = new ModuleRequest();
            req.setEstimatedHours(50);

            var result = performPost("/api/projects/" + projectId + "/modules", req, adminToken);
            assertBizError(result, "模块名称不能为空");
        }

        @Test
        @Order(4)
        void testUpdateModule_success() throws Exception {
            ModuleRequest req = new ModuleRequest();
            req.setName("更新的模块");

            var result = performPut("/api/modules/" + moduleId, req, adminToken);
            assertSuccess(result);
        }

        @Test
        @Order(5)
        void testDeleteModule_success() throws Exception {
            var result = performDelete("/api/modules/" + moduleId, adminToken);
            assertSuccess(result);
        }

        @Test
        @Order(6)
        void testModuleCreate_asUser_forbidden() throws Exception {
            ModuleRequest req = new ModuleRequest();
            req.setName("不应创建");

            var result = performPost("/api/projects/" + projectId + "/modules", req, userToken);
            assertForbidden(result);
        }

        @Test
        @Order(7)
        void testModuleEndpoints_withoutAuth() throws Exception {
            performGet("/api/projects/" + projectId + "/modules", null).andExpect(status().isForbidden());
        }
    }

    // =============================================================
    //  WORK HOUR CONTROLLER (6 endpoints: list, budgetCheck, create, update, delete)
    // =============================================================

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class WorkHourControllerTests {

        @Test
        @Order(1)
        void testListWorkHours_success() throws Exception {
            var result = performGet("/api/work-hours?page=1&size=10", userToken);
            assertSuccess(result);
            result.andExpect(jsonPath("$.data.total").isNumber())
                  .andExpect(jsonPath("$.data.list").isArray());
        }

        @Test
        @Order(2)
        void testListWorkHours_withFilters() throws Exception {
            var result = performGet("/api/work-hours?page=1&size=10&projectId=" + projectId + "&status=PENDING", userToken);
            assertSuccess(result);
            result.andExpect(jsonPath("$.data.total").isNumber());
        }

        @Test
        @Order(3)
        void testListWorkHours_withDateRange() throws Exception {
            var result = performGet("/api/work-hours?page=1&size=10&startDate=2026-01-01&endDate=2026-12-31", userToken);
            assertSuccess(result);
            result.andExpect(jsonPath("$.data.total").isNumber());
        }

        @Test
        @Order(4)
        void testBudgetCheck_withProjectAndModule() throws Exception {
            var result = performGet("/api/work-hours/budget-check?projectId=" + projectId + "&moduleId=" + moduleId, userToken);
            assertSuccess(result);
            result.andExpect(jsonPath("$.data").isMap());
        }

        @Test
        @Order(5)
        void testBudgetCheck_withoutModuleId() throws Exception {
            // budgetCheck requires moduleId, throws BusinessException when null
            var result = performGet("/api/work-hours/budget-check", userToken);
            assertBizError(result, "模块ID不能为空");
        }

        @Test
        @Order(6)
        void testCreateWorkHour_success() throws Exception {
            WorkHourRequest req = new WorkHourRequest();
            req.setProjectId(projectId);
            req.setModuleId(moduleId);
            req.setWorkDate("2026-06-15");
            req.setHours(new BigDecimal("6.5"));
            req.setContent("新填工时");

            var result = performPost("/api/work-hours", req, userToken);
            assertSuccess(result);
            result.andExpect(jsonPath("$.data.id").isNumber());
        }

        @Test
        @Order(7)
        void testCreateWorkHour_missingProject() throws Exception {
            WorkHourRequest req = new WorkHourRequest();
            req.setWorkDate("2026-06-15");
            req.setHours(new BigDecimal("8.0"));

            var result = performPost("/api/work-hours", req, userToken);
            assertBizError(result, "项目不能为空");
        }

        @Test
        @Order(8)
        void testCreateWorkHour_missingDate() throws Exception {
            WorkHourRequest req = new WorkHourRequest();
            req.setProjectId(projectId);
            req.setHours(new BigDecimal("8.0"));

            var result = performPost("/api/work-hours", req, userToken);
            assertBizError(result, "工作日期不能为空");
        }

        @Test
        @Order(9)
        void testCreateWorkHour_invalidHours() throws Exception {
            WorkHourRequest req = new WorkHourRequest();
            req.setProjectId(projectId);
            req.setWorkDate("2026-06-15");
            req.setHours(new BigDecimal("-1"));

            var result = performPost("/api/work-hours", req, userToken);
            assertBizError(result, "工时必须大于0");
        }

        @Test
        @Order(10)
        void testUpdateWorkHour_success() throws Exception {
            WorkHourRequest req = new WorkHourRequest();
            req.setHours(new BigDecimal("5.0"));

            var result = performPut("/api/work-hours/" + workHourId, req, userToken);
            assertSuccess(result);
        }

        @Test
        @Order(11)
        void testDeleteWorkHour_success() throws Exception {
            var result = performDelete("/api/work-hours/" + workHourId, userToken);
            assertSuccess(result);
        }

        @Test
        @Order(12)
        void testCreateWorkHour_asAdmin() throws Exception {
            WorkHourRequest req = new WorkHourRequest();
            req.setProjectId(projectId);
            req.setModuleId(moduleId);
            req.setWorkDate("2026-06-20");
            req.setHours(new BigDecimal("4.0"));
            req.setContent("管理员填报");

            var result = performPost("/api/work-hours", req, adminToken);
            assertSuccess(result);
            result.andExpect(jsonPath("$.data.id").isNumber());
        }

        @Test
        @Order(13)
        void testListWorkHours_asAdmin() throws Exception {
            var result = performGet("/api/work-hours?page=1&size=10", adminToken);
            assertSuccess(result);
        }

        @Test
        @Order(14)
        void testWorkHourEndpoints_withoutAuth() throws Exception {
            performGet("/api/work-hours?page=1&size=10", null).andExpect(status().isForbidden());
        }
    }

    // =============================================================
    //  APPROVAL CONTROLLER (3 endpoints: pending, batch, history)
    // =============================================================

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class ApprovalControllerTests {

        @Test
        @Order(1)
        void testPendingApprovals_asPM() throws Exception {
            var result = performGet("/api/approvals/pending?page=1&size=10", pmToken);
            assertSuccess(result);
            result.andExpect(jsonPath("$.data").isMap());
        }

        @Test
        @Order(2)
        void testPendingApprovals_asDeptManager() throws Exception {
            var result = performGet("/api/approvals/pending?page=1&size=10", deptMgrToken);
            assertSuccess(result);
            result.andExpect(jsonPath("$.data").isMap());
        }

        @Test
        @Order(3)
        void testPendingApprovals_asUser_forbidden() throws Exception {
            var result = performGet("/api/approvals/pending?page=1&size=10", userToken);
            assertForbidden(result);
        }

        @Test
        @Order(4)
        void testPendingApprovals_withoutAuth() throws Exception {
            performGet("/api/approvals/pending?page=1&size=10", null).andExpect(status().isForbidden());
        }

        @Test
        @Order(5)
        void testBatchApprove_success() throws Exception {
            // Create a new work hour for approval (the existing one is already approved)
            WorkHour wh2 = new WorkHour();
            wh2.setUserId(userId);
            wh2.setProjectId(projectId);
            wh2.setModuleId(moduleId);
            wh2.setWorkDate(LocalDate.of(2026, 6, 10));
            wh2.setHours(new BigDecimal("4.0"));
            wh2.setContent("待审批工时");
            wh2.setStatus("PENDING");
            workHourMapper.insert(wh2);

            var items = List.of(Map.of(
                "id", wh2.getId().intValue(),
                "status", "APPROVED",
                "comment", "审批通过"
            ));
            var request = Map.of("items", items);

            var result = performPut("/api/approvals/batch", request, pmToken);
            assertSuccess(result);
        }

        @Test
        @Order(6)
        void testBatchApprove_reject() throws Exception {
            WorkHour wh2 = new WorkHour();
            wh2.setUserId(userId);
            wh2.setProjectId(projectId);
            wh2.setModuleId(moduleId);
            wh2.setWorkDate(LocalDate.of(2026, 6, 10));
            wh2.setHours(new BigDecimal("4.0"));
            wh2.setContent("待审批工时2");
            wh2.setStatus("PENDING");
            workHourMapper.insert(wh2);

            var items = List.of(Map.of(
                "id", wh2.getId().intValue(),
                "status", "REJECTED",
                "comment", "驳回"
            ));
            var request = Map.of("items", items);

            var result = performPut("/api/approvals/batch", request, pmToken);
            assertSuccess(result);
        }

        @Test
        @Order(7)
        void testBatchApprove_asUser_forbidden() throws Exception {
            var request = Map.of("items", List.of());
            var result = performPut("/api/approvals/batch", request, userToken);
            assertForbidden(result);
        }

        @Test
        @Order(8)
        void testApprovalHistory_asPM() throws Exception {
            var result = performGet("/api/approvals/history?page=1&size=10", pmToken);
            assertSuccess(result);
            result.andExpect(jsonPath("$.data").isMap());
        }

        @Test
        @Order(9)
        void testApprovalHistory_asDeptManager() throws Exception {
            var result = performGet("/api/approvals/history?page=1&size=10", deptMgrToken);
            assertSuccess(result);
            result.andExpect(jsonPath("$.data").isMap());
        }

        @Test
        @Order(10)
        void testApprovalHistory_asUser_forbidden() throws Exception {
            var result = performGet("/api/approvals/history?page=1&size=10", userToken);
            assertForbidden(result);
        }
    }

    // =============================================================
    //  DASHBOARD CONTROLLER (5 endpoints: today, pending, monthlyRate, projectDistribution, overview)
    // =============================================================

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class DashboardControllerTests {

        @Test
        @Order(1)
        void testToday_success() throws Exception {
            var result = performGet("/api/dashboard/today", userToken);
            assertSuccess(result);
            result.andExpect(jsonPath("$.data").isMap());
        }

        @Test
        @Order(2)
        void testToday_asAdmin() throws Exception {
            var result = performGet("/api/dashboard/today", adminToken);
            assertSuccess(result);
            result.andExpect(jsonPath("$.data").isMap());
        }

        @Test
        @Order(3)
        void testToday_withoutAuth() throws Exception {
            performGet("/api/dashboard/today", null).andExpect(status().isForbidden());
        }

        @Test
        @Order(4)
        void testPending_success() throws Exception {
            var result = performGet("/api/dashboard/pending", userToken);
            assertSuccess(result);
            result.andExpect(jsonPath("$.data").isMap());
        }

        @Test
        @Order(5)
        void testMonthlyRate_success() throws Exception {
            var result = performGet("/api/dashboard/monthly-rate", userToken);
            assertSuccess(result);
            result.andExpect(jsonPath("$.data").isMap());
        }

        @Test
        @Order(6)
        void testProjectDistribution_success() throws Exception {
            var result = performGet("/api/dashboard/project-distribution", userToken);
            assertSuccess(result);
            result.andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @Order(7)
        void testOverview_success() throws Exception {
            var result = performGet("/api/dashboard/overview", userToken);
            assertSuccess(result);
            result.andExpect(jsonPath("$.data").isMap());
        }

        @Test
        @Order(8)
        void testAllDashboardEndpoints_asUser() throws Exception {
            performGet("/api/dashboard/today", userToken).andExpect(jsonPath("$.code").value(200));
            performGet("/api/dashboard/pending", userToken).andExpect(jsonPath("$.code").value(200));
            performGet("/api/dashboard/monthly-rate", userToken).andExpect(jsonPath("$.code").value(200));
            performGet("/api/dashboard/project-distribution", userToken).andExpect(jsonPath("$.code").value(200));
            performGet("/api/dashboard/overview", userToken).andExpect(jsonPath("$.code").value(200));
        }

        @Test
        @Order(9)
        void testAllDashboardEndpoints_asAdmin() throws Exception {
            performGet("/api/dashboard/today", adminToken).andExpect(jsonPath("$.code").value(200));
            performGet("/api/dashboard/pending", adminToken).andExpect(jsonPath("$.code").value(200));
            performGet("/api/dashboard/monthly-rate", adminToken).andExpect(jsonPath("$.code").value(200));
            performGet("/api/dashboard/project-distribution", adminToken).andExpect(jsonPath("$.code").value(200));
            performGet("/api/dashboard/overview", adminToken).andExpect(jsonPath("$.code").value(200));
        }
    }

    // =============================================================
    //  REPORT CONTROLLER (4 endpoints: personal, project, department, export)
    // =============================================================

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class ReportControllerTests {

        @Test
        @Order(1)
        void testPersonalReport_success() throws Exception {
            var result = performGet("/api/reports/personal?year=2026&month=6", userToken);
            assertSuccess(result);
            result.andExpect(jsonPath("$.data").isMap());
        }

        @Test
        @Order(2)
        void testPersonalReport_asAdmin() throws Exception {
            var result = performGet("/api/reports/personal?year=2026&month=6", adminToken);
            assertSuccess(result);
            result.andExpect(jsonPath("$.data").isMap());
        }

        @Test
        @Order(3)
        void testPersonalReport_withoutAuth() throws Exception {
            performGet("/api/reports/personal?year=2026&month=6", null).andExpect(status().isForbidden());
        }

        @Test
        @Order(4)
        void testProjectReport_success() throws Exception {
            var result = performGet("/api/reports/project?year=2026&month=6&projectId=" + projectId, userToken);
            assertSuccess(result);
            result.andExpect(jsonPath("$.data").isMap());
        }

        @Test
        @Order(5)
        void testDepartmentReport_success() throws Exception {
            var result = performGet("/api/reports/department?year=2026&month=6", userToken);
            assertSuccess(result);
            result.andExpect(jsonPath("$.data").isMap());
        }

        @Test
        @Order(6)
        void testExportExcel_personal() throws Exception {
            var result = performGet("/api/reports/export?type=personal&year=2026&month=6", userToken);
            result.andExpect(status().isOk())
                  .andExpect(header().string("Content-Disposition", "attachment; filename=\"personal_2026_6.xlsx\""))
                  .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        }

        @Test
        @Order(7)
        void testExportExcel_project() throws Exception {
            var result = performGet("/api/reports/export?type=project&year=2026&month=6&projectId=" + projectId, userToken);
            result.andExpect(status().isOk())
                  .andExpect(header().string("Content-Disposition", "attachment; filename=\"project_2026_6.xlsx\""))
                  .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        }

        @Test
        @Order(8)
        void testExportExcel_department() throws Exception {
            var result = performGet("/api/reports/export?type=department&year=2026&month=6", userToken);
            result.andExpect(status().isOk())
                  .andExpect(header().string("Content-Disposition", "attachment; filename=\"department_2026_6.xlsx\""))
                  .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        }

        @Test
        @Order(9)
        void testReports_withoutAuth() throws Exception {
            performGet("/api/reports/personal?year=2026&month=6", null).andExpect(status().isForbidden());
            performGet("/api/reports/project?year=2026&month=6&projectId=1", null).andExpect(status().isForbidden());
        }
    }

    // =============================================================
    //  SYSTEM CONTROLLER (logs) (1 endpoint: list)
    // =============================================================

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class SystemControllerTests {

        @Test
        @Order(1)
        void testListLogs_success() throws Exception {
            var result = performGet("/api/logs?page=1&size=10", adminToken);
            assertSuccess(result);
            result.andExpect(jsonPath("$.data.total").isNumber())
                  .andExpect(jsonPath("$.data.list").isArray());
        }

        @Test
        @Order(2)
        void testListLogs_withFilters() throws Exception {
            var result = performGet("/api/logs?page=1&size=10&action=LOGIN&userId=" + adminId, adminToken);
            assertSuccess(result);
            result.andExpect(jsonPath("$.data.total").isNumber());
        }

        @Test
        @Order(3)
        void testListLogs_withDateRange() throws Exception {
            var result = performGet("/api/logs?page=1&size=10&startDate=2026-01-01&endDate=2027-01-01", adminToken);
            assertSuccess(result);
        }

        @Test
        @Order(4)
        void testListLogs_asUser_forbidden() throws Exception {
            var result = performGet("/api/logs?page=1&size=10", userToken);
            assertForbidden(result);
        }

        @Test
        @Order(5)
        void testListLogs_asPM_forbidden() throws Exception {
            var result = performGet("/api/logs?page=1&size=10", pmToken);
            assertForbidden(result);
        }

        @Test
        @Order(6)
        void testListLogs_asDeptManager_forbidden() throws Exception {
            var result = performGet("/api/logs?page=1&size=10", deptMgrToken);
            assertForbidden(result);
        }

        @Test
        @Order(7)
        void testListLogs_withoutAuth() throws Exception {
            performGet("/api/logs?page=1&size=10", null).andExpect(status().isForbidden());
        }
    }

    // =============================================================
    //  AUTHENTICATION / AUTHORIZATION EDGE CASES
    // =============================================================

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class AuthEdgeCaseTests {

        @Test
        @Order(1)
        void testInvalidToken() throws Exception {
            mockMvc.perform(get("/api/work-hours?page=1&size=10")
                    .header("Authorization", "Bearer invalid_token_here")
                    .contentType(MediaType.APPLICATION_JSON))
                   .andExpect(status().isForbidden());
        }

        @Test
        @Order(2)
        void testNoTokenOnProtectedEndpoint() throws Exception {
            mockMvc.perform(get("/api/work-hours?page=1&size=10")
                    .contentType(MediaType.APPLICATION_JSON))
                   .andExpect(status().isForbidden());
        }

        @Test
        @Order(3)
        void testMalformedAuthorizationHeader() throws Exception {
            mockMvc.perform(get("/api/work-hours?page=1&size=10")
                    .header("Authorization", "NotBearer token")
                    .contentType(MediaType.APPLICATION_JSON))
                   .andExpect(status().isForbidden());
        }

        @Test
        @Order(4)
        void testPublicEndpoint_noAuth() throws Exception {
            // Verify that the public /api/auth/** endpoints are accessible without auth
            // Captcha endpoint doesn't require auth
            mockMvc.perform(get("/api/auth/captcha"))
                   .andExpect(status().isOk());
        }

        @Test
        @Order(5)
        void testLogin_withEmptyPassword() throws Exception {
            // password is null/empty - @Valid will fail, returning 500 from GlobalExceptionHandler
            LoginRequest req = new LoginRequest();
            req.setUsername(ADMIN_USERNAME);
            var result = performPost("/api/auth/login", req, null);
            // @Valid validation fails -> MethodArgumentNotValidException not handled -> 500
            result.andExpect(status().isInternalServerError());
        }
    }

    // =============================================================
    //  CONTROLLER VALIDATION PATHS (BusinessException handlers)
    // =============================================================

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class BusinessErrorTests {

        @Test
        @Order(1)
        void testUpdateModule_notFound() throws Exception {
            ModuleRequest req = new ModuleRequest();
            req.setName("不存在的模块");

            var result = performPut("/api/modules/99999", req, adminToken);
            result.andExpect(status().isOk());
        }

        @Test
        @Order(2)
        void testDeleteProject_notFound() throws Exception {
            var result = performDelete("/api/projects/99999", adminToken);
            assertBizError(result, "项目不存在");
        }

        @Test
        @Order(3)
        void testUpdateWorkHour_notFound() throws Exception {
            WorkHourRequest req = new WorkHourRequest();
            req.setHours(new BigDecimal("5.0"));

            var result = performPut("/api/work-hours/99999", req, userToken);
            assertBizError(result, "工时记录不存在");
        }

        @Test
        @Order(4)
        void testDeleteWorkHour_notFound() throws Exception {
            var result = performDelete("/api/work-hours/99999", userToken);
            assertBizError(result, "工时记录不存在");
        }

        @Test
        @Order(5)
        void testApproval_pendingWithEmptyResult() throws Exception {
            var result = performGet("/api/approvals/pending?page=1&size=10", pmToken);
            assertSuccess(result);
        }
    }
}
