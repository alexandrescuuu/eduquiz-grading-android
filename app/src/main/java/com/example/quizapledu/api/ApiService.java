package com.example.quizapledu.api;

import com.example.quizapledu.api.models.*;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;
import java.util.List;
public interface ApiService {

    @POST("api/auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("api/test/create")
    Call<TestCreateResponse> createTest(@Body TestCreateRequest request);

    @GET("api/test/list")
    Call<TestListResponse> getTests();

    @GET("api/test/list")
    Call<TestListResponse> getTestsByTeacher(@Query("teacher_id") int teacherId);

    @GET("api/test/{test_id}/questions")
    Call<QuestionsResponse> getQuestions(@Path("test_id") int testId);

    @POST("api/test/submit")
    Call<SubmitResponse> submitTest(@Body SubmitRequest request);

    @Multipart
    @POST("api/test/scan")
    Call<ResponseBody> scanTest(
            @Part MultipartBody.Part image,
            @Part("test_id") RequestBody testId,
            @Part("student_id") RequestBody studentId
    );

    @Multipart
    @POST("api/test/scan")
    Call<ResponseBody> scanQuestion(
            @Part MultipartBody.Part image,
            @Part("test_id") RequestBody testId,
            @Part("student_id") RequestBody studentId,
            @Part("question_id") RequestBody questionId
    );

    @Multipart
    @POST("api/test/scan-full-page")
    Call<ResponseBody> scanFullPage(
            @Part MultipartBody.Part image,
            @Part("test_id") RequestBody testId,
            @Part("student_id") RequestBody studentId
    );

    @GET("api/submission/{submission_id}")
    Call<SubmissionDetail> getSubmissionDetails(@Path("submission_id") int submissionId);

    @GET("api/test/{test_id}/results")
    Call<ResponseBody> getTestResults(@Path("test_id") int testId);

    @GET("api/analytics/common-errors/{test_id}")
    Call<ResponseBody> getCommonErrors(@Path("test_id") int testId);

    // ---- DATE REALE STUDENT / TEACHER ----

    @GET("api/student/{student_id}/dashboard")
    Call<ResponseBody> getStudentDashboard(@Path("student_id") int studentId);

    @GET("api/student/{student_id}/progress")
    Call<ResponseBody> getStudentProgress(@Path("student_id") int studentId);

    @GET("api/teacher/{teacher_id}/dashboard")
    Call<ResponseBody> getTeacherDashboard(@Path("teacher_id") int teacherId);

    @GET("api/teacher/{teacher_id}/classes")
    Call<ResponseBody> getTeacherClasses(@Path("teacher_id") int teacherId);

    @GET("api/teacher/{teacher_id}/analytics")
    Call<ResponseBody> getTeacherAnalytics(@Path("teacher_id") int teacherId);

    // ---- ADMIN ----

    @GET("api/admin/overview")
    Call<ResponseBody> getAdminOverview();

    @GET("api/admin/users")
    Call<ResponseBody> getAdminUsers();

    @GET("api/admin/reports")
    Call<ResponseBody> getAdminReports();

    // ---- SETTINGS ----

    @PUT("api/user/{user_id}/change-password")
    Call<ResponseBody> changePassword(
            @Path("user_id") int userId,
            @Query("current_password") String currentPassword,
            @Query("new_password") String newPassword
    );

    @GET("api/test/{test_id}/analysis")
    Call<ResponseBody> getTestAnalysis(@Path("test_id") int testId);

    // OMR Scan - detectie X sau cerc
    @Multipart
    @POST("api/test/scan-omr")
    Call<ResponseBody> scanOMR(
            @Part MultipartBody.Part image,
            @Part("test_id") RequestBody testId,
            @Part("student_id") RequestBody studentId
    );

    // Batch Scan - mai multe foi
    @Multipart
    @POST("api/test/batch-scan")
    Call<ResponseBody> batchScan(
            @Part List<MultipartBody.Part> images,
            @Part("test_id") RequestBody testId,
            @Part("student_ids") RequestBody studentIds
    );

}
