package com.timemanager.service;

import com.timemanager.common.BusinessException;
import com.timemanager.dto.*;
import com.timemanager.entity.*;
import com.timemanager.mapper.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive Spring Boot integration tests for all services.
 * Uses real MySQL database (not H2), rolls back after each test.
 * Sets up SecurityContextHolder for authentication simulation.
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AllServicesIntegrationTest {

    @Autowired private AuthService authService;
    @Autowired private UserService userService;
    @Autowired private ProjectService projectService;
    @Autowired private ModuleService moduleService;
    @Autowired private WorkHourService workHourService;
    @Autowired private ApprovalService approvalService;
    @Autowired private LogService logService;
    @Autowired private DashboardService dashboardService;
    @Autowired private ReportService reportService;

    @Autowired private UserMapper userMapper;
    @Autowired private ProjectMapper projectMapper;
    @Autowired private TaskModuleMapper moduleMapper;
    @Autowired private WorkHourMapper workHourMapper;
    @Autowired private ApprovalMapper approvalMapper;
    @Autowired private OperationLogMapper operationLogMapper;
    @Autowired private BCryptPasswordEncoder encoder;

    // Test data IDs
    private static Long adminId;
    private static Long pmId;
    private static Long userId;
    private static Long otherManagerId;
    private static Long projectId;
    private static Long moduleId;
    private static Long workHourId;

    private static final String ADMIN_USERNAME = "test_admin";
    private static final String PM_USERNAME = "test_pm";
    private static final String USER_USERNAME = "test_user";
    private static final String OTHER_MGR_USERNAME = "other_mgr";
    private static final String PASSWORD = "123456";

    // ========== Authentication Setup ==========

    private void setSecurityContext(String username, Long userId, String role) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                username, null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
        auth.setDetails(userId);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void setAdminAuth() {
        setSecurityContext(ADMIN_USERNAME, adminId, "ADMIN");
    }

    private void setPmAuth() {
        setSecurityContext(PM_USERNAME, pmId, "PM");
    }

    private void setUserAuth() {
        setSecurityContext(USER_USERNAME, userId, "USER");
    }

    private void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    // ========== Test Data Setup ==========

    @BeforeEach
    void setUp() {
        // Create admin user
        User admin = new User();
        admin.setUsername(ADMIN_USERNAME);
        admin.setPassword(encoder.encode(PASSWORD));
        admin.setName("管理员");
        admin.setEmail("admin@test.com");
        admin.setPhone("13800000001");
        admin.setDepartment("研发与交付中心");
        admin.setRole("ADMIN");
        admin.setStatus(1);
        admin.setFirstLogin(0);
        userMapper.insert(admin);
        adminId = admin.getId();

        // Create PM user
        User pm = new User();
        pm.setUsername(PM_USERNAME);
        pm.setPassword(encoder.encode(PASSWORD));
        pm.setName("项目经理");
        pm.setEmail("pm@test.com");
        pm.setPhone("13800000002");
        pm.setDepartment("研发与交付中心");
        pm.setRole("PM");
        pm.setStatus(1);
        pm.setFirstLogin(0);
        userMapper.insert(pm);
        pmId = pm.getId();

        // Create regular user
        User user = new User();
        user.setUsername(USER_USERNAME);
        user.setPassword(encoder.encode(PASSWORD));
        user.setName("普通用户");
        user.setEmail("user@test.com");
        user.setPhone("13800000003");
        user.setDepartment("研发与交付中心");
        user.setRole("USER");
        user.setStatus(1);
        user.setFirstLogin(1);
        userMapper.insert(user);
        userId = user.getId();

        // Create another PM (for "other manager" scenarios)
        User otherMgr = new User();
        otherMgr.setUsername(OTHER_MGR_USERNAME);
        otherMgr.setPassword(encoder.encode(PASSWORD));
        otherMgr.setName("其他项目经理");
        otherMgr.setEmail("otherpm@test.com");
        otherMgr.setPhone("13800000004");
        otherMgr.setDepartment("研发与交付中心");
        otherMgr.setRole("PM");
        otherMgr.setStatus(1);
        otherMgr.setFirstLogin(0);
        userMapper.insert(otherMgr);
        otherManagerId = otherMgr.getId();

        // Set admin auth by default
        setAdminAuth();
    }

    @AfterEach
    void tearDown() {
        clearAuth();
    }

    // =============================================================
    //  AUTH SERVICE TESTS
    // =============================================================

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class AuthServiceTests {

        @Test
        @Transactional
        @Order(1)
        void testLogin_success() {
            LoginRequest req = new LoginRequest();
            req.setUsername(ADMIN_USERNAME);
            req.setPassword(PASSWORD);
            // LoginRequest has captcha fields but the service doesn't validate them
            req.setCaptchaId("dummy");
            req.setCaptchaCode("dummy");

            LoginResponse resp = authService.login(req);
            assertNotNull(resp);
            assertNotNull(resp.getToken());
            assertEquals(adminId, resp.getUserId());
            assertEquals(ADMIN_USERNAME, resp.getUsername());
            assertEquals("管理员", resp.getName());
            assertEquals("ADMIN", resp.getRole());
            assertFalse(resp.getFirstLogin());
        }

        @Test
        @Transactional
        @Order(2)
        void testLogin_wrongPassword() {
            LoginRequest req = new LoginRequest();
            req.setUsername(ADMIN_USERNAME);
            req.setPassword("wrong_password");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> authService.login(req));
            assertEquals("用户名或密码错误", ex.getMessage());
        }

        @Test
        @Transactional
        @Order(3)
        void testLogin_userNotFound() {
            LoginRequest req = new LoginRequest();
            req.setUsername("nonexistent_user");
            req.setPassword(PASSWORD);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> authService.login(req));
            assertEquals("用户名或密码错误", ex.getMessage());
        }

        @Test
        @Transactional
        @Order(4)
        void testLogin_disabledUser() {
            // Create a disabled user
            User disabledUser = new User();
            disabledUser.setUsername("disabled_user");
            disabledUser.setPassword(encoder.encode(PASSWORD));
            disabledUser.setName("已禁用用户");
            disabledUser.setRole("USER");
            disabledUser.setStatus(0); // disabled
            disabledUser.setFirstLogin(0);
            userMapper.insert(disabledUser);

            LoginRequest req = new LoginRequest();
            req.setUsername("disabled_user");
            req.setPassword(PASSWORD);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> authService.login(req));
            assertEquals("账户已被禁用", ex.getMessage());
        }

        @Test
        @Transactional
        @Order(5)
        void testLogin_firstLogin() {
            // user is already set with firstLogin=1 in setUp
            LoginRequest req = new LoginRequest();
            req.setUsername(USER_USERNAME);
            req.setPassword(PASSWORD);

            LoginResponse resp = authService.login(req);
            assertTrue(resp.getFirstLogin());
        }

        @Test
        @Transactional
        @Order(6)
        void testChangePassword_success() {
            setUserAuth();
            PasswordRequest req = new PasswordRequest();
            req.setOldPassword(PASSWORD);
            req.setNewPassword("newpass123");

            authService.changePassword(req);

            // Verify password changed by logging in with new password
            LoginRequest loginReq = new LoginRequest();
            loginReq.setUsername(USER_USERNAME);
            loginReq.setPassword("newpass123");
            LoginResponse resp = authService.login(loginReq);
            assertNotNull(resp.getToken());
            // firstLogin should be 0 now
            assertFalse(resp.getFirstLogin());
        }

        @Test
        @Transactional
        @Order(7)
        void testChangePassword_wrongOldPassword() {
            setUserAuth();
            PasswordRequest req = new PasswordRequest();
            req.setOldPassword("wrong_old_password");
            req.setNewPassword("newpass123");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> authService.changePassword(req));
            assertEquals("旧密码错误", ex.getMessage());
        }

        @Test
        @Transactional
        @Order(8)
        void testChangePassword_userNotFoundFromAuth() {
            // Set auth with nonexistent username
            setSecurityContext("nonexistent", 99999L, "USER");
            PasswordRequest req = new PasswordRequest();
            req.setOldPassword(PASSWORD);
            req.setNewPassword("newpass123");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> authService.changePassword(req));
            assertEquals("用户不存在", ex.getMessage());
        }
    }

    // =============================================================
    //  USER SERVICE TESTS
    // =============================================================

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class UserServiceTests {

        @Test
        @Transactional
        @Order(1)
        void testCreateUser_success() {
            UserRequest req = new UserRequest();
            req.setUsername("new_user");
            req.setName("新用户");
            req.setEmail("new@test.com");
            req.setPhone("13800000100");
            req.setDepartment("测试部门");
            req.setRole("USER");
            req.setStatus(1);

            User created = userService.createUser(req);
            assertNotNull(created);
            assertNotNull(created.getId());
            assertEquals("new_user", created.getUsername());
            assertEquals("新用户", created.getName());
            assertEquals("USER", created.getRole());
            assertEquals(1, created.getFirstLogin().intValue());

            // Verify it's actually in the DB
            User fromDb = userMapper.findByUsername("new_user");
            assertNotNull(fromDb);
            assertTrue(encoder.matches("123456", fromDb.getPassword()));
        }

        @Test
        @Transactional
        @Order(2)
        void testCreateUser_duplicateUsername() {
            UserRequest req = new UserRequest();
            req.setUsername(ADMIN_USERNAME); // already exists
            req.setName("重复用户");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.createUser(req));
            assertEquals("用户名已存在", ex.getMessage());
        }

        @Test
        @Transactional
        @Order(3)
        void testCreateUser_defaultValues() {
            UserRequest req = new UserRequest();
            req.setUsername("minimal_user");
            req.setName("最小用户");

            User created = userService.createUser(req);
            assertEquals("研发与交付中心", created.getDepartment());
            assertEquals("USER", created.getRole());
            assertEquals(1, created.getStatus().intValue());
            assertEquals(1, created.getFirstLogin().intValue());
        }

        @Test
        @Transactional
        @Order(4)
        void testUpdateUser_success() {
            UserRequest req = new UserRequest();
            req.setName("更新的用户");
            req.setEmail("updated@test.com");
            req.setPhone("13800000999");
            req.setDepartment("新部门");
            req.setRole("PM");
            req.setStatus(0);

            userService.updateUser(userId, req);

            User fromDb = userMapper.findById(userId);
            assertEquals("更新的用户", fromDb.getName());
            assertEquals("updated@test.com", fromDb.getEmail());
            assertEquals("13800000999", fromDb.getPhone());
            assertEquals("新部门", fromDb.getDepartment());
            assertEquals("PM", fromDb.getRole());
            assertEquals(0, fromDb.getStatus().intValue());
        }

        @Test
        @Transactional
        @Order(5)
        void testUpdateUser_partialUpdate() {
            UserRequest req = new UserRequest();
            req.setName("仅更新名字");

            userService.updateUser(userId, req);

            User fromDb = userMapper.findById(userId);
            assertEquals("仅更新名字", fromDb.getName());
            // Other fields should remain
            assertEquals(USER_USERNAME, fromDb.getUsername());
        }

        @Test
        @Transactional
        @Order(6)
        void testUpdateUser_notFound() {
            UserRequest req = new UserRequest();
            req.setName("不存在");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.updateUser(99999L, req));
            assertEquals("用户不存在", ex.getMessage());
        }

        @Test
        @Transactional
        @Order(7)
        void testDeleteUser_success() {
            // Create a disposable user
            User disposable = new User();
            disposable.setUsername("delete_me");
            disposable.setPassword(encoder.encode(PASSWORD));
            disposable.setName("待删除用户");
            disposable.setRole("USER");
            disposable.setStatus(1);
            disposable.setFirstLogin(0);
            userMapper.insert(disposable);

            userService.deleteUser(disposable.getId());

            User fromDb = userMapper.findById(disposable.getId());
            assertNull(fromDb); // logically deleted, not found with is_deleted=0

            // Verify by checking the raw record
            List<User> all = userMapper.findAll();
            assertTrue(all.stream().noneMatch(u -> u.getId().equals(disposable.getId())));
        }

        @Test
        @Transactional
        @Order(8)
        void testDeleteUser_notFound() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.deleteUser(99999L));
            assertEquals("用户不存在", ex.getMessage());
        }

        @Test
        @Transactional
        @Order(9)
        void testUpdateRole_success() {
            userService.updateRole(userId, "PM");

            User fromDb = userMapper.findById(userId);
            assertEquals("PM", fromDb.getRole());
        }

        @Test
        @Transactional
        @Order(10)
        void testUpdateRole_notFound() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.updateRole(99999L, "ADMIN"));
            assertEquals("用户不存在", ex.getMessage());
        }

        @Test
        @Transactional
        @Order(11)
        void testResetPassword_success() {
            userService.resetPassword(userId);

            User fromDb = userMapper.findById(userId);
            assertTrue(encoder.matches("123456", fromDb.getPassword()));
            assertEquals(1, fromDb.getFirstLogin().intValue());
        }

        @Test
        @Transactional
        @Order(12)
        void testResetPassword_notFound() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> userService.resetPassword(99999L));
            assertEquals("用户不存在", ex.getMessage());
        }

        @Test
        @Transactional
        @Order(13)
        void testListUsers_pagination() {
            PageResult<User> result = userService.listUsers(1, 10, null);
            assertTrue(result.getTotal() >= 3); // admin, pm, user
            assertTrue(result.getList().size() >= 3);
        }

        @Test
        @Transactional
        @Order(14)
        void testListUsers_withKeyword() {
            PageResult<User> result = userService.listUsers(1, 10, "管理员");
            assertTrue(result.getTotal() >= 1);
            assertTrue(result.getList().stream().anyMatch(u -> u.getName().contains("管理员")));
        }

        @Test
        @Transactional
        @Order(15)
        void testListByRole() {
            List<User> admins = userService.listByRole("ADMIN");
            assertTrue(admins.stream().anyMatch(u -> u.getId().equals(adminId)));

            List<User> users = userService.listByRole("USER");
            assertTrue(users.stream().anyMatch(u -> u.getId().equals(userId)));
        }

        @Test
        @Transactional
        @Order(16)
        void testListByRole_inactiveUsersExcluded() {
            // Create a disabled USER
            User disabled = new User();
            disabled.setUsername("disabled_role_user");
            disabled.setPassword(encoder.encode(PASSWORD));
            disabled.setName("禁用用户");
            disabled.setRole("USER");
            disabled.setStatus(0); // disabled
            disabled.setFirstLogin(0);
            userMapper.insert(disabled);

            List<User> users = userService.listByRole("USER");
            assertTrue(users.stream().noneMatch(u -> u.getId().equals(disabled.getId())));
        }
    }

    // =============================================================
    //  PROJECT SERVICE TESTS
    // =============================================================

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class ProjectServiceTests {

        private Long createTestProject() {
            ProjectRequest req = new ProjectRequest();
            req.setName("测试项目");
            req.setDescription("这是一个测试项目");
            req.setManagerId(pmId);
            req.setStartDate("2026-01-01");
            req.setEndDate("2026-12-31");
            req.setStatus("ACTIVE");

            Project project = projectService.createProject(req);
            projectId = project.getId();
            return project.getId();
        }

        @Test
        @Transactional
        @Order(1)
        void testCreateProject_success() {
            createTestProject();

            Project fromDb = projectMapper.findById(projectId);
            assertNotNull(fromDb);
            assertEquals("测试项目", fromDb.getName());
            assertEquals(pmId, fromDb.getManagerId());
            assertEquals("ACTIVE", fromDb.getStatus());
            assertEquals(LocalDate.of(2026, 1, 1), fromDb.getStartDate());
            assertEquals(LocalDate.of(2026, 12, 31), fromDb.getEndDate());
        }

        @Test
        @Transactional
        @Order(2)
        void testCreateProject_blankName() {
            ProjectRequest req = new ProjectRequest();
            req.setName("");
            req.setManagerId(pmId);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> projectService.createProject(req));
            assertEquals("项目名称不能为空", ex.getMessage());
        }

        @Test
        @Transactional
        @Order(3)
        void testCreateProject_nullName() {
            ProjectRequest req = new ProjectRequest();
            req.setManagerId(pmId);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> projectService.createProject(req));
            assertEquals("项目名称不能为空", ex.getMessage());
        }

        @Test
        @Transactional
        @Order(4)
        void testCreateProject_nullManagerId() {
            ProjectRequest req = new ProjectRequest();
            req.setName("无经理项目");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> projectService.createProject(req));
            assertEquals("项目经理不能为空", ex.getMessage());
        }

        @Test
        @Transactional
        @Order(5)
        void testCreateProject_defaultStatus() {
            ProjectRequest req = new ProjectRequest();
            req.setName("默认状态项目");
            req.setManagerId(pmId);

            Project project = projectService.createProject(req);
            assertEquals("ACTIVE", project.getStatus());
        }

        @Test
        @Transactional
        @Order(6)
        void testUpdateProject_success() {
            createTestProject();

            ProjectRequest req = new ProjectRequest();
            req.setName("更新后的项目");
            req.setDescription("更新描述");
            req.setStatus("FINISHED");

            projectService.updateProject(projectId, req);

            Project fromDb = projectMapper.findById(projectId);
            assertEquals("更新后的项目", fromDb.getName());
            assertEquals("FINISHED", fromDb.getStatus());
        }

        @Test
        @Transactional
        @Order(7)
        void testUpdateProject_notFound() {
            ProjectRequest req = new ProjectRequest();
            req.setName("更新不存在");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> projectService.updateProject(99999L, req));
            assertEquals("项目不存在", ex.getMessage());
        }

        @Test
        @Transactional
        @Order(8)
        void testDeleteProject_success() {
            createTestProject();
            projectService.deleteProject(projectId);

            assertNull(projectMapper.findById(projectId));
        }

        @Test
        @Transactional
        @Order(9)
        void testDeleteProject_notFound() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> projectService.deleteProject(99999L));
            assertEquals("项目不存在", ex.getMessage());
        }

        @Test
        @Transactional
        @Order(10)
        void testGetProjectById_success() {
            createTestProject();
            Project project = projectService.getProjectById(projectId);
            assertNotNull(project);
            assertEquals("测试项目", project.getName());
            assertNotNull(project.getManagerName()); // should have manager name from JOIN
        }

        @Test
        @Transactional
        @Order(11)
        void testGetProjectById_notFound() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> projectService.getProjectById(99999L));
            assertEquals("项目不存在", ex.getMessage());
        }

        @Test
        @Transactional
        @Order(12)
        void testListProjects() {
            createTestProject();
            PageResult<Project> result = projectService.listProjects(1, 10, null);
            assertTrue(result.getTotal() >= 1);
            assertTrue(result.getList().stream().anyMatch(p -> p.getName().equals("测试项目")));
        }

        @Test
        @Transactional
        @Order(13)
        void testListProjects_withKeyword() {
            createTestProject();
            PageResult<Project> result = projectService.listProjects(1, 10, "测试项目");
            assertTrue(result.getTotal() >= 1);
        }
    }

    // =============================================================
    //  MODULE SERVICE TESTS
    // =============================================================

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class ModuleServiceTests {

        private Long createTestProject() {
            Project project = new Project();
            project.setName("模块测试项目");
            project.setManagerId(pmId);
            project.setStatus("ACTIVE");
            projectMapper.insert(project);
            return project.getId();
        }

        @Test
        @Transactional
        @Order(1)
        void testCreateModule_success() {
            Long projId = createTestProject();

            ModuleRequest req = new ModuleRequest();
            req.setName("需求分析");
            req.setDescription("需求分析阶段");
            req.setEstimatedHours(40);

            TaskModule module = moduleService.createModule(projId, req);
            assertNotNull(module);
            assertNotNull(module.getId());
            assertEquals("需求分析", module.getName());
            assertEquals(40, module.getEstimatedHours().intValue());

            moduleId = module.getId();
        }

        @Test
        @Transactional
        @Order(2)
        void testCreateModule_blankName() {
            ModuleRequest req = new ModuleRequest();
            req.setName("");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> moduleService.createModule(1L, req));
            assertEquals("模块名称不能为空", ex.getMessage());
        }

        @Test
        @Transactional
        @Order(3)
        void testCreateModule_nullName() {
            ModuleRequest req = new ModuleRequest();

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> moduleService.createModule(1L, req));
            assertEquals("模块名称不能为空", ex.getMessage());
        }

        @Test
        @Transactional
        @Order(4)
        void testCreateModule_defaultEstimatedHours() {
            Long projId = createTestProject();
            ModuleRequest req = new ModuleRequest();
            req.setName("默认工时模块");

            TaskModule module = moduleService.createModule(projId, req);
            assertEquals(0, module.getEstimatedHours().intValue());
        }

        @Test
        @Transactional
        @Order(5)
        void testUpdateModule_success() {
            Long projId = createTestProject();
            ModuleRequest createReq = new ModuleRequest();
            createReq.setName("原始模块");
            createReq.setEstimatedHours(20);
            TaskModule module = moduleService.createModule(projId, createReq);

            ModuleRequest updateReq = new ModuleRequest();
            updateReq.setName("更新后的模块");
            updateReq.setDescription("更新描述");
            updateReq.setEstimatedHours(50);

            moduleService.updateModule(module.getId(), updateReq);

            TaskModule fromDb = moduleMapper.findById(module.getId());
            assertEquals("更新后的模块", fromDb.getName());
            assertEquals(50, fromDb.getEstimatedHours().intValue());
        }

        @Test
        @Transactional
        @Order(6)
        void testUpdateModule_notFound() {
            ModuleRequest req = new ModuleRequest();
            req.setName("不存在");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> moduleService.updateModule(99999L, req));
            assertEquals("模块不存在", ex.getMessage());
        }

        @Test
        @Transactional
        @Order(7)
        void testDeleteModule_success() {
            Long projId = createTestProject();
            ModuleRequest createReq = new ModuleRequest();
            createReq.setName("待删除模块");
            TaskModule module = moduleService.createModule(projId, createReq);

            moduleService.deleteModule(module.getId());

            assertNull(moduleMapper.findById(module.getId()));
        }

        @Test
        @Transactional
        @Order(8)
        void testDeleteModule_notFound() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> moduleService.deleteModule(99999L));
            assertEquals("模块不存在", ex.getMessage());
        }

        @Test
        @Transactional
        @Order(9)
        void testListModules() {
            Long projId = createTestProject();

            ModuleRequest req1 = new ModuleRequest();
            req1.setName("模块一");
            ModuleRequest req2 = new ModuleRequest();
            req2.setName("模块二");

            moduleService.createModule(projId, req1);
            moduleService.createModule(projId, req2);

            List<TaskModule> modules = moduleService.listModules(projId);
            assertEquals(2, modules.size());
        }

        @Test
        @Transactional
        @Order(10)
        void testListModules_emptyProject() {
            List<TaskModule> modules = moduleService.listModules(99999L);
            assertTrue(modules.isEmpty());
        }
    }

    // =============================================================
    //  WORK HOUR SERVICE TESTS
    // =============================================================

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class WorkHourServiceTests {

        private Long createProject() {
            Project project = new Project();
            project.setName("工时测试项目");
            project.setManagerId(pmId);
            project.setStatus("ACTIVE");
            projectMapper.insert(project);
            return project.getId();
        }

        private Long createModule(Long projId, int estimatedHours) {
            TaskModule module = new TaskModule();
            module.setProjectId(projId);
            module.setName("测试模块");
            module.setEstimatedHours(estimatedHours);
            moduleMapper.insert(module);
            return module.getId();
        }

        @Test
        @Transactional
        @Order(1)
        void testCreateWorkHour_success() {
            Long projId = createProject();
            Long modId = createModule(projId, 100);

            setUserAuth();

            WorkHourRequest req = new WorkHourRequest();
            req.setProjectId(projId);
            req.setModuleId(modId);
            req.setWorkDate("2026-06-15");
            req.setHours(new BigDecimal("8"));
            req.setContent("完成了登录功能开发");

            WorkHour wh = workHourService.createWorkHour(req, userId, "USER");
            assertNotNull(wh);
            assertNotNull(wh.getId());
            assertEquals(userId, wh.getUserId());
            assertEquals(projId, wh.getProjectId());
            assertEquals(modId, wh.getModuleId());
            assertEquals(LocalDate.of(2026, 6, 15), wh.getWorkDate());
            assertEquals(new BigDecimal("8"), wh.getHours());
            assertEquals("PENDING", wh.getStatus());

            workHourId = wh.getId();
        }

        @Test
        @Transactional
        @Order(2)
        void testCreateWorkHour_nullProjectId() {
            setUserAuth();
            WorkHourRequest req = new WorkHourRequest();
            req.setWorkDate("2026-06-15");
            req.setHours(new BigDecimal("8"));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> workHourService.createWorkHour(req, userId, "USER"));
            assertEquals("项目不能为空", ex.getMessage());
        }

        @Test
        @Transactional
        @Order(3)
        void testCreateWorkHour_nullWorkDate() {
            Long projId = createProject();
            setUserAuth();
            WorkHourRequest req = new WorkHourRequest();
            req.setProjectId(projId);
            req.setHours(new BigDecimal("8"));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> workHourService.createWorkHour(req, userId, "USER"));
            assertEquals("工作日期不能为空", ex.getMessage());
        }

        @Test
        @Transactional
        @Order(4)
        void testCreateWorkHour_hoursZeroOrNegative() {
            Long projId = createProject();
            setUserAuth();

            WorkHourRequest req = new WorkHourRequest();
            req.setProjectId(projId);
            req.setWorkDate("2026-06-15");
            req.setHours(BigDecimal.ZERO);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> workHourService.createWorkHour(req, userId, "USER"));
            assertEquals("工时必须大于0", ex.getMessage());

            req.setHours(new BigDecimal("-1"));
            ex = assertThrows(BusinessException.class,
                    () -> workHourService.createWorkHour(req, userId, "USER"));
            assertEquals("工时必须大于0", ex.getMessage());
        }

        @Test
        @Transactional
        @Order(5)
        void testCreateWorkHour_userRequiresModuleId() {
            Long projId = createProject();
            setUserAuth();

            WorkHourRequest req = new WorkHourRequest();
            req.setProjectId(projId);
            req.setWorkDate("2026-06-15");
            req.setHours(new BigDecimal("8"));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> workHourService.createWorkHour(req, userId, "USER"));
            assertEquals("请选择任务模块", ex.getMessage());
        }

        @Test
        @Transactional
        @Order(6)
        void testCreateWorkHour_moduleNotFound() {
            Long projId = createProject();
            setUserAuth();

            WorkHourRequest req = new WorkHourRequest();
            req.setProjectId(projId);
            req.setModuleId(99999L);
            req.setWorkDate("2026-06-15");
            req.setHours(new BigDecimal("8"));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> workHourService.createWorkHour(req, userId, "USER"));
            assertEquals("模块不存在", ex.getMessage());
        }

        @Test
        @Transactional
        @Order(7)
        void testCreateWorkHour_budgetExceeded() {
            Long projId = createProject();
            Long modId = createModule(projId, 10); // Only 10 hours estimated
            setUserAuth();

            // First: use 8 hours
            WorkHourRequest req1 = new WorkHourRequest();
            req1.setProjectId(projId);
            req1.setModuleId(modId);
            req1.setWorkDate("2026-06-15");
            req1.setHours(new BigDecimal("8"));
            workHourService.createWorkHour(req1, userId, "USER");

            // Second: try to add 5 more (total 13 > 10)
            WorkHourRequest req2 = new WorkHourRequest();
            req2.setProjectId(projId);
            req2.setModuleId(modId);
            req2.setWorkDate("2026-06-16");
            req2.setHours(new BigDecimal("5"));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> workHourService.createWorkHour(req2, userId, "USER"));
            assertEquals("该模块预算已达上限", ex.getMessage());
        }

        @Test
        @Transactional
        @Order(8)
        void testCreateWorkHour_adminNoModuleRequired() {
            Long projId = createProject();
            setAdminAuth();

            WorkHourRequest req = new WorkHourRequest();
            req.setProjectId(projId);
            req.setWorkDate("2026-06-15");
            req.setHours(new BigDecimal("8"));

            WorkHour wh = workHourService.createWorkHour(req, adminId, "ADMIN");
            assertNotNull(wh);
            assertNull(wh.getModuleId()); // Admin didn't need to specify module
        }

        @Test
        @Transactional
        @Order(9)
        void testUpdateWorkHour_success() {
            Long projId = createProject();
            Long modId = createModule(projId, 100);
            setUserAuth();

            // Create
            WorkHourRequest createReq = new WorkHourRequest();
            createReq.setProjectId(projId);
            createReq.setModuleId(modId);
            createReq.setWorkDate("2026-06-15");
            createReq.setHours(new BigDecimal("8"));
            WorkHour wh = workHourService.createWorkHour(createReq, userId, "USER");

            // Update
            WorkHourRequest updateReq = new WorkHourRequest();
            updateReq.setHours(new BigDecimal("6"));
            updateReq.setContent("更新后的内容");

            workHourService.updateWorkHour(wh.getId(), updateReq);

            WorkHour fromDb = workHourMapper.findById(wh.getId());
            assertEquals(0, new BigDecimal("6").compareTo(fromDb.getHours()));
            assertEquals("更新后的内容", fromDb.getContent());
            assertEquals("PENDING", fromDb.getStatus()); // Reset to PENDING
        }

        @Test
        @Transactional
        @Order(10)
        void testUpdateWorkHour_approvedRecord() {
            Long projId = createProject();
            Long modId = createModule(projId, 100);
            setUserAuth();

            WorkHourRequest createReq = new WorkHourRequest();
            createReq.setProjectId(projId);
            createReq.setModuleId(modId);
            createReq.setWorkDate("2026-06-15");
            createReq.setHours(new BigDecimal("8"));
            WorkHour wh = workHourService.createWorkHour(createReq, userId, "USER");

            // Manually set status to APPROVED
            workHourMapper.updateStatus(wh.getId(), "APPROVED");

            WorkHourRequest updateReq = new WorkHourRequest();
            updateReq.setHours(new BigDecimal("6"));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> workHourService.updateWorkHour(wh.getId(), updateReq));
            assertEquals("已审批的工时不可修改", ex.getMessage());
        }

        @Test
        @Transactional
        @Order(11)
        void testUpdateWorkHour_notFound() {
            WorkHourRequest req = new WorkHourRequest();
            req.setHours(new BigDecimal("6"));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> workHourService.updateWorkHour(99999L, req));
            assertEquals("工时记录不存在", ex.getMessage());
        }

        @Test
        @Transactional
        @Order(12)
        void testDeleteWorkHour_success() {
            Long projId = createProject();
            Long modId = createModule(projId, 100);
            setUserAuth();

            WorkHourRequest createReq = new WorkHourRequest();
            createReq.setProjectId(projId);
            createReq.setModuleId(modId);
            createReq.setWorkDate("2026-06-15");
            createReq.setHours(new BigDecimal("8"));
            WorkHour wh = workHourService.createWorkHour(createReq, userId, "USER");

            workHourService.deleteWorkHour(wh.getId());
            assertNull(workHourMapper.findById(wh.getId()));
        }

        @Test
        @Transactional
        @Order(13)
        void testDeleteWorkHour_approvedRecord() {
            Long projId = createProject();
            Long modId = createModule(projId, 100);
            setUserAuth();

            WorkHourRequest createReq = new WorkHourRequest();
            createReq.setProjectId(projId);
            createReq.setModuleId(modId);
            createReq.setWorkDate("2026-06-15");
            createReq.setHours(new BigDecimal("8"));
            WorkHour wh = workHourService.createWorkHour(createReq, userId, "USER");

            workHourMapper.updateStatus(wh.getId(), "APPROVED");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> workHourService.deleteWorkHour(wh.getId()));
            assertEquals("已审批的工时不可删除", ex.getMessage());
        }

        @Test
        @Transactional
        @Order(14)
        void testDeleteWorkHour_notFound() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> workHourService.deleteWorkHour(99999L));
            assertEquals("工时记录不存在", ex.getMessage());
        }

        @Test
        @Transactional
        @Order(15)
        void testListWorkHours_admin() {
            Long projId = createProject();
            Long modId = createModule(projId, 100);
            setUserAuth();

            WorkHourRequest createReq = new WorkHourRequest();
            createReq.setProjectId(projId);
            createReq.setModuleId(modId);
            createReq.setWorkDate("2026-06-15");
            createReq.setHours(new BigDecimal("8"));
            workHourService.createWorkHour(createReq, userId, "USER");

            setAdminAuth();
            PageResult<WorkHour> result = workHourService.listWorkHours(
                    1, 10, adminId, "ADMIN", null, null, null, null);
            assertTrue(result.getTotal() >= 1);
        }

        @Test
        @Transactional
        @Order(16)
        void testListWorkHours_userSeesOnlyOwn() {
            Long projId = createProject();
            Long modId = createModule(projId, 100);
            setUserAuth();

            WorkHourRequest createReq = new WorkHourRequest();
            createReq.setProjectId(projId);
            createReq.setModuleId(modId);
            createReq.setWorkDate("2026-06-15");
            createReq.setHours(new BigDecimal("8"));
            workHourService.createWorkHour(createReq, userId, "USER");

            // User lists their own hours
            PageResult<WorkHour> result = workHourService.listWorkHours(
                    1, 10, userId, "USER", null, null, null, null);
            assertTrue(result.getTotal() >= 1);

            // PM lists hours (should not see user's hours)
            setPmAuth();
            PageResult<WorkHour> pmResult = workHourService.listWorkHours(
                    1, 10, pmId, "PM", null, null, null, null);
            // PM's own hours - should be 0 since PM never created any
            assertEquals(0, pmResult.getTotal());
        }

        @Test
        @Transactional
        @Order(17)
        void testListWorkHours_withFilters() {
            Long projId = createProject();
            Long modId = createModule(projId, 100);
            setUserAuth();

            WorkHourRequest createReq = new WorkHourRequest();
            createReq.setProjectId(projId);
            createReq.setModuleId(modId);
            createReq.setWorkDate("2026-06-15");
            createReq.setHours(new BigDecimal("8"));
            workHourService.createWorkHour(createReq, userId, "USER");

            setAdminAuth();
            PageResult<WorkHour> result = workHourService.listWorkHours(
                    1, 10, adminId, "ADMIN", projId,
                    "2026-06-01", "2026-06-30", "PENDING");
            assertTrue(result.getTotal() >= 1);
        }

        @Test
        @Transactional
        @Order(18)
        void testBudgetCheck_success() {
            Long projId = createProject();
            Long modId = createModule(projId, 100);

            Map<String, Object> budget = workHourService.budgetCheck(projId, modId);
            assertEquals(0, ((BigDecimal) budget.get("estimatedHours")).compareTo(new BigDecimal("100")));
            assertEquals(0, ((BigDecimal) budget.get("usedHours")).compareTo(BigDecimal.ZERO));
            assertEquals(0, ((BigDecimal) budget.get("remainingHours")).compareTo(new BigDecimal("100")));
            assertFalse((Boolean) budget.get("locked"));
        }

        @Test
        @Transactional
        @Order(19)
        void testBudgetCheck_afterWorkHours() {
            Long projId = createProject();
            Long modId = createModule(projId, 100);
            setUserAuth();

            WorkHourRequest createReq = new WorkHourRequest();
            createReq.setProjectId(projId);
            createReq.setModuleId(modId);
            createReq.setWorkDate("2026-06-15");
            createReq.setHours(new BigDecimal("30"));
            workHourService.createWorkHour(createReq, userId, "USER");

            setAdminAuth();
            Map<String, Object> budget = workHourService.budgetCheck(projId, modId);
            assertEquals(0, ((BigDecimal) budget.get("estimatedHours")).compareTo(new BigDecimal("100")));
            assertEquals(0, ((BigDecimal) budget.get("usedHours")).compareTo(new BigDecimal("30")));
            assertEquals(0, ((BigDecimal) budget.get("remainingHours")).compareTo(new BigDecimal("70")));
            assertFalse((Boolean) budget.get("locked"));
        }

        @Test
        @Transactional
        @Order(20)
        void testBudgetCheck_locked() {
            Long projId = createProject();
            Long modId = createModule(projId, 10);
            setUserAuth();

            WorkHourRequest createReq = new WorkHourRequest();
            createReq.setProjectId(projId);
            createReq.setModuleId(modId);
            createReq.setWorkDate("2026-06-15");
            createReq.setHours(new BigDecimal("10"));
            workHourService.createWorkHour(createReq, userId, "USER");

            setAdminAuth();
            Map<String, Object> budget = workHourService.budgetCheck(projId, modId);
            assertEquals(0, ((BigDecimal) budget.get("estimatedHours")).compareTo(new BigDecimal("10")));
            assertEquals(0, ((BigDecimal) budget.get("usedHours")).compareTo(new BigDecimal("10")));
            assertEquals(0, ((BigDecimal) budget.get("remainingHours")).compareTo(BigDecimal.ZERO));
            assertTrue((Boolean) budget.get("locked"));
        }

        @Test
        @Transactional
        @Order(21)
        void testBudgetCheck_nullModuleId() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> workHourService.budgetCheck(1L, null));
            assertEquals("模块ID不能为空", ex.getMessage());
        }

        @Test
        @Transactional
        @Order(22)
        void testBudgetCheck_moduleNotFound() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> workHourService.budgetCheck(1L, 99999L));
            assertEquals("模块不存在", ex.getMessage());
        }
    }

    // =============================================================
    //  LOG SERVICE TESTS
    // =============================================================

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class LogServiceTests {

        @Test
        @Transactional
        @Order(1)
        void testSaveLog() {
            logService.save(adminId, "LOGIN", "用户登录", "{\"username\":\"test_admin\"}");

            PageResult<OperationLog> result = logService.listLogs(1, 10, null, null, null, null);
            assertTrue(result.getTotal() >= 1);
        }

        @Test
        @Transactional
        @Order(2)
        void testListLogs_withFilters() {
            logService.save(adminId, "LOGIN", "用户登录", "{\"username\":\"test_admin\"}");
            logService.save(adminId, "CREATE", "创建用户", "{}");

            PageResult<OperationLog> result = logService.listLogs(1, 10, adminId, null, null, null);
            assertTrue(result.getTotal() >= 2);

            result = logService.listLogs(1, 10, adminId, "LOGIN", null, null);
            assertEquals(1, result.getList().size());
        }

        @Test
        @Transactional
        @Order(3)
        void testListLogs_withDateRange() {
            logService.save(adminId, "LOGIN", "用户登录", "{}");

            String today = LocalDate.now().toString();
            PageResult<OperationLog> result = logService.listLogs(
                    1, 10, null, null, today, today);
            assertTrue(result.getTotal() >= 1);
        }

        @Test
        @Transactional
        @Order(4)
        void testListLogs_emptyResult() {
            PageResult<OperationLog> result = logService.listLogs(
                    1, 10, 99999L, null, null, null);
            assertEquals(0, result.getTotal());
        }
    }

    // =============================================================
    //  APPROVAL SERVICE TESTS
    // =============================================================

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class ApprovalServiceTests {

        private Long createProjectWithManager(Long managerId) {
            Project project = new Project();
            project.setName("审批测试项目");
            project.setManagerId(managerId);
            project.setStatus("ACTIVE");
            projectMapper.insert(project);
            return project.getId();
        }

        private Long createModule(Long projId) {
            TaskModule module = new TaskModule();
            module.setProjectId(projId);
            module.setName("审批模块");
            module.setEstimatedHours(100);
            moduleMapper.insert(module);
            return module.getId();
        }

        private Long createWorkHour(Long projId, Long modId, Long submitterId) {
            WorkHour wh = new WorkHour();
            wh.setUserId(submitterId);
            wh.setProjectId(projId);
            wh.setModuleId(modId);
            wh.setWorkDate(LocalDate.of(2026, 6, 15));
            wh.setHours(new BigDecimal("8"));
            wh.setContent("测试工时");
            wh.setStatus("PENDING");
            workHourMapper.insert(wh);
            return wh.getId();
        }

        @Test
        @Transactional
        @Order(1)
        void testBatchApprove_pmApprovesUser() {
            // Arrange: PM manages a project, a USER submits hours
            Long projId = createProjectWithManager(pmId);
            Long modId = createModule(projId);
            Long whId = createWorkHour(projId, modId, userId);

            setPmAuth();

            List<Map<String, Object>> items = new ArrayList<>();
            Map<String, Object> item = new HashMap<>();
            item.put("id", whId);
            item.put("status", "APPROVED");
            item.put("comment", "审批通过");
            items.add(item);

            approvalService.batchApprove(items, pmId, "PM");

            // Verify
            WorkHour wh = workHourMapper.findById(whId);
            assertEquals("APPROVED", wh.getStatus());

            // Approval record should exist
            var history = approvalService.getApprovalHistory(1, 10, pmId);
            assertEquals(1L, history.get("total"));
        }

        @Test
        @Transactional
        @Order(2)
        void testBatchApprove_pmRejectsUser() {
            Long projId = createProjectWithManager(pmId);
            Long modId = createModule(projId);
            Long whId = createWorkHour(projId, modId, userId);

            setPmAuth();

            List<Map<String, Object>> items = new ArrayList<>();
            Map<String, Object> item = new HashMap<>();
            item.put("id", whId);
            item.put("status", "REJECTED");
            item.put("comment", "驳回");
            items.add(item);

            approvalService.batchApprove(items, pmId, "PM");

            WorkHour wh = workHourMapper.findById(whId);
            assertEquals("REJECTED", wh.getStatus());
        }

        @Test
        @Transactional
        @Order(3)
        void testBatchApprove_deptManagerApprovesPm() {
            // Create a dept manager user
            User deptMgr = new User();
            deptMgr.setUsername("dept_mgr");
            deptMgr.setPassword(encoder.encode(PASSWORD));
            deptMgr.setName("部门经理");
            deptMgr.setRole("DEPT_MANAGER");
            deptMgr.setStatus(1);
            deptMgr.setFirstLogin(0);
            userMapper.insert(deptMgr);
            Long deptMgrId = deptMgr.getId();

            Long projId = createProjectWithManager(pmId);
            Long modId = createModule(projId);

            // PM submits hours as a regular submitter
            Long whId = createWorkHour(projId, modId, pmId);

            setSecurityContext("dept_mgr", deptMgrId, "DEPT_MANAGER");

            List<Map<String, Object>> items = new ArrayList<>();
            Map<String, Object> item = new HashMap<>();
            item.put("id", whId);
            item.put("status", "APPROVED");
            item.put("comment", "部门经理审批通过");
            items.add(item);

            approvalService.batchApprove(items, deptMgrId, "DEPT_MANAGER");

            WorkHour wh = workHourMapper.findById(whId);
            assertEquals("APPROVED", wh.getStatus());
        }

        @Test
        @Transactional
        @Order(4)
        void testBatchApprove_emptyList() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> approvalService.batchApprove(new ArrayList<>(), pmId, "PM"));
            assertEquals("审批列表不能为空", ex.getMessage());
        }

        @Test
        @Transactional
        @Order(5)
        void testBatchApprove_nullId() {
            List<Map<String, Object>> items = new ArrayList<>();
            Map<String, Object> item = new HashMap<>();
            item.put("status", "APPROVED");
            items.add(item);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> approvalService.batchApprove(items, pmId, "PM"));
            assertEquals("审批记录ID不能为空", ex.getMessage());
        }

        @Test
        @Transactional
        @Order(6)
        void testBatchApprove_invalidStatus() {
            List<Map<String, Object>> items = new ArrayList<>();
            Map<String, Object> item = new HashMap<>();
            item.put("id", 1L);
            item.put("status", "INVALID_STATUS");
            items.add(item);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> approvalService.batchApprove(items, pmId, "PM"));
            assertEquals("审批状态无效", ex.getMessage());
        }

        @Test
        @Transactional
        @Order(7)
        void testBatchApprove_workHourNotFound() {
            List<Map<String, Object>> items = new ArrayList<>();
            Map<String, Object> item = new HashMap<>();
            item.put("id", 99999L);
            item.put("status", "APPROVED");
            items.add(item);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> approvalService.batchApprove(items, pmId, "PM"));
            assertTrue(ex.getMessage().contains("工时记录不存在"));
        }

        @Test
        @Transactional
        @Order(8)
        void testBatchApprove_alreadyProcessed() {
            Long projId = createProjectWithManager(pmId);
            Long modId = createModule(projId);
            Long whId = createWorkHour(projId, modId, userId);
            workHourMapper.updateStatus(whId, "APPROVED");

            setPmAuth();

            List<Map<String, Object>> items = new ArrayList<>();
            Map<String, Object> item = new HashMap<>();
            item.put("id", whId);
            item.put("status", "APPROVED");
            items.add(item);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> approvalService.batchApprove(items, pmId, "PM"));
            assertTrue(ex.getMessage().contains("状态不是待审批"));
        }

        @Test
        @Transactional
        @Order(9)
        void testBatchApprove_pmCannotApproveOtherPmProject() {
            // Create project managed by otherManagerId (not our pmId)
            Project otherProject = new Project();
            otherProject.setName("其他项目");
            otherProject.setManagerId(otherManagerId); // some other manager
            otherProject.setStatus("ACTIVE");
            projectMapper.insert(otherProject);

            Long modId = createModule(otherProject.getId());
            Long whId = createWorkHour(otherProject.getId(), modId, userId);

            setPmAuth();

            List<Map<String, Object>> items = new ArrayList<>();
            Map<String, Object> item = new HashMap<>();
            item.put("id", whId);
            item.put("status", "APPROVED");
            items.add(item);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> approvalService.batchApprove(items, pmId, "PM"));
            assertTrue(ex.getMessage().contains("无权审批该项目工时"));
        }

        @Test
        @Transactional
        @Order(10)
        void testBatchApprove_pmCannotApprovePmHours() {
            Long projId = createProjectWithManager(pmId);
            Long modId = createModule(projId);
            // PM submits hours on their own project
            Long whId = createWorkHour(projId, modId, pmId);

            setPmAuth();

            List<Map<String, Object>> items = new ArrayList<>();
            Map<String, Object> item = new HashMap<>();
            item.put("id", whId);
            item.put("status", "APPROVED");
            items.add(item);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> approvalService.batchApprove(items, pmId, "PM"));
            assertEquals("无权审批项目经理的工时", ex.getMessage());
        }

        @Test
        @Transactional
        @Order(11)
        void testBatchApprove_deptManagerCannotApproveUserHours() {
            // Create a dept manager user
            User deptMgr = new User();
            deptMgr.setUsername("dept_mgr2");
            deptMgr.setPassword(encoder.encode(PASSWORD));
            deptMgr.setName("部门经理2");
            deptMgr.setRole("DEPT_MANAGER");
            deptMgr.setStatus(1);
            deptMgr.setFirstLogin(0);
            userMapper.insert(deptMgr);
            Long deptMgrId = deptMgr.getId();

            Long projId = createProjectWithManager(pmId);
            Long modId = createModule(projId);
            Long whId = createWorkHour(projId, modId, userId);

            setSecurityContext("dept_mgr2", deptMgrId, "DEPT_MANAGER");

            List<Map<String, Object>> items = new ArrayList<>();
            Map<String, Object> item = new HashMap<>();
            item.put("id", whId);
            item.put("status", "APPROVED");
            items.add(item);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> approvalService.batchApprove(items, deptMgrId, "DEPT_MANAGER"));
            assertEquals("部门经理只能审批项目经理的工时", ex.getMessage());
        }

        @Test
        @Transactional
        @Order(12)
        void testGetPendingApprovals_pm() {
            Long projId = createProjectWithManager(pmId);
            Long modId = createModule(projId);
            createWorkHour(projId, modId, userId);

            setPmAuth();
            Map<String, Object> pending = approvalService.getPendingApprovals(1, 10, pmId, "PM");
            assertTrue((Long) pending.get("total") >= 1);
        }

        @Test
        @Transactional
        @Order(13)
        void testGetPendingApprovals_deptManager() {
            // Create a dept manager user
            User deptMgr = new User();
            deptMgr.setUsername("dept_mgr_pending");
            deptMgr.setPassword(encoder.encode(PASSWORD));
            deptMgr.setName("部门经理3");
            deptMgr.setRole("DEPT_MANAGER");
            deptMgr.setStatus(1);
            deptMgr.setFirstLogin(0);
            userMapper.insert(deptMgr);
            Long deptMgrId = deptMgr.getId();

            Long projId = createProjectWithManager(pmId);
            Long modId = createModule(projId);
            createWorkHour(projId, modId, pmId); // PM submits hours

            setSecurityContext("dept_mgr_pending", deptMgrId, "DEPT_MANAGER");
            Map<String, Object> pending = approvalService.getPendingApprovals(1, 10, deptMgrId, "DEPT_MANAGER");
            assertTrue((Long) pending.get("total") >= 1);
        }

        @Test
        @Transactional
        @Order(14)
        void testGetPendingApprovals_noPermission() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> approvalService.getPendingApprovals(1, 10, userId, "USER"));
            assertEquals("无审批权限", ex.getMessage());
        }

        @Test
        @Transactional
        @Order(15)
        void testGetApprovalHistory() {
            // First approve something
            Long projId = createProjectWithManager(pmId);
            Long modId = createModule(projId);
            Long whId = createWorkHour(projId, modId, userId);

            setPmAuth();
            List<Map<String, Object>> items = new ArrayList<>();
            Map<String, Object> item = new HashMap<>();
            item.put("id", whId);
            item.put("status", "APPROVED");
            items.add(item);
            approvalService.batchApprove(items, pmId, "PM");

            var history = approvalService.getApprovalHistory(1, 10, pmId);
            assertTrue((Long) history.get("total") >= 1);
        }
    }

    // =============================================================
    //  DASHBOARD SERVICE TESTS
    // =============================================================

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class DashboardServiceTests {

        @Test
        @Transactional
        @Order(1)
        void testToday_user() {
            Map<String, Object> data = dashboardService.today(userId, "USER");
            assertNotNull(data);
            assertTrue(data.containsKey("submitCount"));
            assertTrue(data.containsKey("totalHours"));
        }

        @Test
        @Transactional
        @Order(2)
        void testToday_admin() {
            Map<String, Object> data = dashboardService.today(adminId, "ADMIN");
            assertNotNull(data);
        }

        @Test
        @Transactional
        @Order(3)
        void testPending_pm() {
            Map<String, Object> data = dashboardService.pending(pmId, "PM");
            assertTrue(data.containsKey("count"));
        }

        @Test
        @Transactional
        @Order(4)
        void testPending_deptManager() {
            // Create a dept manager for this test
            User deptMgr = new User();
            deptMgr.setUsername("dash_dept_mgr");
            deptMgr.setPassword(encoder.encode(PASSWORD));
            deptMgr.setName("仪表盘部门经理");
            deptMgr.setRole("DEPT_MANAGER");
            deptMgr.setStatus(1);
            deptMgr.setFirstLogin(0);
            userMapper.insert(deptMgr);

            Map<String, Object> data = dashboardService.pending(deptMgr.getId(), "DEPT_MANAGER");
            assertTrue(data.containsKey("count"));
        }

        @Test
        @Transactional
        @Order(5)
        void testPending_user() {
            Map<String, Object> data = dashboardService.pending(userId, "USER");
            assertEquals(0L, data.get("count"));
        }

        @Test
        @Transactional
        @Order(6)
        void testMonthlyRate_user() {
            Map<String, Object> data = dashboardService.monthlyRate(userId, "USER");
            assertTrue(data.containsKey("totalHours"));
            assertTrue(data.containsKey("rate"));
        }

        @Test
        @Transactional
        @Order(7)
        void testMonthlyRate_admin() {
            Map<String, Object> data = dashboardService.monthlyRate(adminId, "ADMIN");
            assertTrue(data.containsKey("totalHours"));
            assertTrue(data.containsKey("rate"));
        }

        @Test
        @Transactional
        @Order(8)
        void testMonthlyRate_pm() {
            Map<String, Object> data = dashboardService.monthlyRate(pmId, "PM");
            assertTrue(data.containsKey("totalHours"));
            assertTrue(data.containsKey("rate"));
        }

        @Test
        @Transactional
        @Order(9)
        void testProjectDistribution_user() {
            var data = dashboardService.projectDistribution(userId, "USER");
            assertNotNull(data);
        }

        @Test
        @Transactional
        @Order(10)
        void testProjectDistribution_admin() {
            var data = dashboardService.projectDistribution(adminId, "ADMIN");
            assertNotNull(data);
        }

        @Test
        @Transactional
        @Order(11)
        void testOverview_user() {
            Map<String, Object> data = dashboardService.overview(userId, "USER");
            assertTrue(data.containsKey("totalUsers"));
            assertTrue(data.containsKey("totalHours"));
            assertTrue(data.containsKey("avgHours"));
            assertEquals(1L, data.get("totalUsers")); // USER sees self
        }

        @Test
        @Transactional
        @Order(12)
        void testOverview_admin() {
            Map<String, Object> data = dashboardService.overview(adminId, "ADMIN");
            assertTrue(data.containsKey("totalUsers"));
            assertTrue((Long) data.get("totalUsers") >= 3); // admin, pm, user
        }
    }

    // =============================================================
    //  REPORT SERVICE TESTS
    // =============================================================

    @Nested
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    class ReportServiceTests {

        private LocalDate now = LocalDate.now();
        private int year = now.getYear();
        private int month = now.getMonthValue();

        private Long createProjectWithManager(Long managerId) {
            Project project = new Project();
            project.setName("报表测试项目");
            project.setManagerId(managerId);
            project.setStatus("ACTIVE");
            project.setStartDate(LocalDate.of(year, month, 1));
            project.setEndDate(LocalDate.of(year, month, 1).plusMonths(1));
            projectMapper.insert(project);
            return project.getId();
        }

        private Long createModule(Long projId, int estimatedHours) {
            TaskModule module = new TaskModule();
            module.setProjectId(projId);
            module.setName("报表模块");
            module.setEstimatedHours(estimatedHours);
            moduleMapper.insert(module);
            return module.getId();
        }

        private void createWorkHour(Long projId, Long modId, Long submitterId, BigDecimal hours) {
            WorkHour wh = new WorkHour();
            wh.setUserId(submitterId);
            wh.setProjectId(projId);
            wh.setModuleId(modId);
            wh.setWorkDate(now);
            wh.setHours(hours);
            wh.setContent("报表测试");
            wh.setStatus("APPROVED");
            workHourMapper.insert(wh);
        }

        @Test
        @Transactional
        @Order(1)
        void testPersonalReport_user() {
            Long projId = createProjectWithManager(pmId);
            Long modId = createModule(projId, 100);
            createWorkHour(projId, modId, userId, new BigDecimal("8"));

            Map<String, Object> report = reportService.personalReport(year, month, userId, "USER");
            assertTrue(((BigDecimal) report.get("totalHours")).compareTo(BigDecimal.ZERO) > 0);
            assertFalse(((List<?>) report.get("details")).isEmpty());
        }

        @Test
        @Transactional
        @Order(2)
        void testPersonalReport_userWithNoHours() {
            Map<String, Object> report = reportService.personalReport(year, month, userId, "USER");
            assertEquals(0, ((BigDecimal) report.get("totalHours")).compareTo(BigDecimal.ZERO));
        }

        @Test
        @Transactional
        @Order(3)
        void testPersonalReport_pm() {
            Long projId = createProjectWithManager(pmId);
            Long modId = createModule(projId, 100);
            createWorkHour(projId, modId, pmId, new BigDecimal("8"));

            Map<String, Object> report = reportService.personalReport(year, month, pmId, "PM");
            assertTrue(((BigDecimal) report.get("totalHours")).compareTo(BigDecimal.ZERO) > 0);
        }

        @Test
        @Transactional
        @Order(4)
        void testPersonalReport_admin() {
            Long projId = createProjectWithManager(pmId);
            Long modId = createModule(projId, 100);
            createWorkHour(projId, modId, userId, new BigDecimal("8"));

            // Admin sees department view (all data)
            Map<String, Object> report = reportService.personalReport(year, month, adminId, "ADMIN");
            assertTrue(((BigDecimal) report.get("totalHours")).compareTo(BigDecimal.ZERO) > 0);
        }

        @Test
        @Transactional
        @Order(5)
        void testProjectReport_admin() {
            Long projId = createProjectWithManager(pmId);
            Long modId = createModule(projId, 100);
            createWorkHour(projId, modId, userId, new BigDecimal("8"));

            Map<String, Object> report = reportService.projectReport(year, month, projId, adminId, "ADMIN");
            assertTrue(((BigDecimal) report.get("totalHours")).compareTo(BigDecimal.ZERO) > 0);
        }

        @Test
        @Transactional
        @Order(6)
        void testProjectReport_pmOwnProject() {
            Long projId = createProjectWithManager(pmId);
            Long modId = createModule(projId, 100);
            createWorkHour(projId, modId, userId, new BigDecimal("8"));

            Map<String, Object> report = reportService.projectReport(year, month, projId, pmId, "PM");
            assertTrue(((BigDecimal) report.get("totalHours")).compareTo(BigDecimal.ZERO) > 0);
        }

        @Test
        @Transactional
        @Order(7)
        void testProjectReport_pmOtherProject() {
            // Project managed by another PM
            Project otherProject = new Project();
            otherProject.setName("其他人的项目");
            otherProject.setManagerId(otherManagerId);
            otherProject.setStatus("ACTIVE");
            projectMapper.insert(otherProject);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> reportService.projectReport(year, month, otherProject.getId(), pmId, "PM"));
            assertEquals("无权查看此项目统计", ex.getMessage());
        }

        @Test
        @Transactional
        @Order(8)
        void testDepartmentReport_admin() {
            Long projId = createProjectWithManager(pmId);
            Long modId = createModule(projId, 100);
            createWorkHour(projId, modId, userId, new BigDecimal("8"));

            Map<String, Object> report = reportService.departmentReport(year, month, adminId, "ADMIN");
            assertTrue(((BigDecimal) report.get("totalHours")).compareTo(BigDecimal.ZERO) > 0);
            assertTrue((Long) report.get("userCount") >= 1);
        }

        @Test
        @Transactional
        @Order(9)
        void testDepartmentReport_pm() {
            Long projId = createProjectWithManager(pmId);
            Long modId = createModule(projId, 100);
            createWorkHour(projId, modId, userId, new BigDecimal("8"));

            Map<String, Object> report = reportService.departmentReport(year, month, pmId, "PM");
            assertTrue(((BigDecimal) report.get("totalHours")).compareTo(BigDecimal.ZERO) > 0);
        }

        @Test
        @Transactional
        @Order(10)
        void testDepartmentReport_user() {
            Long projId = createProjectWithManager(pmId);
            Long modId = createModule(projId, 100);
            createWorkHour(projId, modId, userId, new BigDecimal("8"));

            Map<String, Object> report = reportService.departmentReport(year, month, userId, "USER");
            assertTrue(((BigDecimal) report.get("totalHours")).compareTo(BigDecimal.ZERO) > 0);
            assertEquals(1L, report.get("userCount"));
        }

        @Test
        @Transactional
        @Order(11)
        void testExportExcel_personal() {
            Long projId = createProjectWithManager(pmId);
            Long modId = createModule(projId, 100);
            createWorkHour(projId, modId, userId, new BigDecimal("8"));

            byte[] excel = reportService.exportExcel("personal", year, month, userId, "USER");
            assertNotNull(excel);
            assertTrue(excel.length > 0);

            // Verify it's a valid XSSFWorkbook
            try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(
                    new java.io.ByteArrayInputStream(excel))) {
                assertEquals(1, workbook.getNumberOfSheets());
            } catch (Exception e) {
                fail("Should produce a valid Excel workbook: " + e.getMessage());
            }
        }

        @Test
        @Transactional
        @Order(12)
        void testExportExcel_department() {
            Long projId = createProjectWithManager(pmId);
            Long modId = createModule(projId, 100);
            createWorkHour(projId, modId, userId, new BigDecimal("8"));

            byte[] excel = reportService.exportExcel("department", year, month, adminId, "ADMIN");
            assertNotNull(excel);
            assertTrue(excel.length > 0);
        }

        @Test
        @Transactional
        @Order(13)
        void testExportExcel_project() {
            Long projId = createProjectWithManager(pmId);
            Long modId = createModule(projId, 100);
            createWorkHour(projId, modId, userId, new BigDecimal("8"));

            byte[] excel = reportService.exportExcel("project", year, month, adminId, "ADMIN");
            assertNotNull(excel);
            // Project export without projectId returns empty list
        }

        @Test
        @Transactional
        @Order(14)
        void testExportProjectExcel_admin() {
            Long projId = createProjectWithManager(pmId);
            Long modId = createModule(projId, 100);
            createWorkHour(projId, modId, userId, new BigDecimal("8"));

            byte[] excel = reportService.exportProjectExcel(year, month, projId, adminId, "ADMIN");
            assertNotNull(excel);
            assertTrue(excel.length > 0);

            try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(
                    new java.io.ByteArrayInputStream(excel))) {
                assertEquals(1, workbook.getNumberOfSheets());
            } catch (Exception e) {
                fail("Should produce a valid Excel workbook: " + e.getMessage());
            }
        }

        @Test
        @Transactional
        @Order(15)
        void testExportProjectExcel_pmOwnProject() {
            Long projId = createProjectWithManager(pmId);
            Long modId = createModule(projId, 100);
            createWorkHour(projId, modId, userId, new BigDecimal("8"));

            byte[] excel = reportService.exportProjectExcel(year, month, projId, pmId, "PM");
            assertNotNull(excel);
            assertTrue(excel.length > 0);
        }

        @Test
        @Transactional
        @Order(16)
        void testExportProjectExcel_pmOtherProject() {
            Project otherProject = new Project();
            otherProject.setName("其他人的项目");
            otherProject.setManagerId(otherManagerId);
            otherProject.setStatus("ACTIVE");
            projectMapper.insert(otherProject);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> reportService.exportProjectExcel(year, month, otherProject.getId(), pmId, "PM"));
            assertTrue(ex.getMessage().contains("无权查看此项目统计"));
        }
    }
}
