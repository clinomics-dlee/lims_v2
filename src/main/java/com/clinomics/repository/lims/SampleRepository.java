package com.clinomics.repository.lims;

import java.util.List;
import java.util.Map;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.clinomics.entity.lims.Sample;
import com.clinomics.enums.StatusCode;

public interface SampleRepository extends JpaRepository<Sample, Integer>, JpaSpecificationExecutor<Sample> {
    List<Sample> findByIdIn(List<Integer> id);
    List<Sample> findByIdInAndStatusCodeIn(List<Integer> id, List<StatusCode> statusCodes);
    //Optional<Sample> findTopByBundle_IdOrderByLaboratoryIdDesc(int bundleId);
    //Optional<Sample> findTopByBundle_IdAndReceivedDateOrderByLaboratoryIdDesc(int bundleId, LocalDate receivedDate);
    
	@Query(value = "SELECT MAX(laboratory_id) FROM sample WHERE is_test = 0 AND bundle_id = ?1", nativeQuery = true)
	String findMaxLaboratoryId(int bundleId);
	@Query(value = "SELECT MAX(laboratory_id) FROM sample WHERE is_test = 0 AND bundle_id = ?1 AND DATE_FORMAT(received_date, '%Y%m') = ?2", nativeQuery = true)
	String findMaxHospitalLaboratoryId(int bundleId, String yyyymm);
    @Query(value = "SELECT MAX(management_number) FROM sample WHERE is_test = 0 AND bundle_id = ?1", nativeQuery = true)
	String findMaxManagementNumber(int bundleId);
    @Query(value = "SELECT DISTINCT REPLACE(JSON_EXTRACT(items, '$.h_name'), '\"', '') AS name FROM sample WHERE is_test = 0 AND JSON_EXTRACT(items, '$.h_name') IS NOT NULL", nativeQuery = true)
    List<String> findDistinctHospitalName();

    @Query(value = "SELECT dayofmonth(s.created_date) AS c1, COUNT(*) AS c2"
                + " , SUM(CASE WHEN s.output_cmpl_date IS NULL THEN 0 ELSE 1 END) AS c3"
                + " FROM sample s INNER JOIN bundle b ON s.bundle_id = b.id AND b.is_active = 1 "
                + " WHERE s.created_date BETWEEN :sDate AND :fDate "
                + " AND s.is_test = 0 "
                + " AND s.is_last_version = 1 "
                + " AND s.bundle_id in :bundleIds "
                + " AND s.status_code in :statusCode "
                + " GROUP BY dayofmonth(s.created_date) ", nativeQuery = true)
    List<String[]> findCalendarDataByCreatedDate(@Param("sDate") String sDate, @Param("fDate") String fDate
                                            , @Param("bundleIds") List<Integer> bundleIds
                                            , @Param("statusCode") List<String> statusCodes);

    @Query(value = "SELECT dayofmonth(s.modified_date) AS c1, COUNT(*) AS c2"
                + " FROM sample s INNER JOIN bundle b ON s.bundle_id = b.id AND b.is_active = 1 "
                + " WHERE s.modified_date BETWEEN :sDate AND :fDate "
                + " AND s.is_test = 0 "
                + " AND s.is_last_version = 1 "
                + " AND s.bundle_id in :bundleIds "
                + " AND s.status_code in :statusCode "
                + " GROUP BY dayofmonth(s.modified_date) ", nativeQuery = true)
    List<String[]> findCalendarDataByModifiedDate(@Param("sDate") String sDate, @Param("fDate") String fDate
                                            , @Param("bundleIds") List<Integer> bundleIds
                                            , @Param("statusCode") List<String> statusCodes);

    @Query(value = "SELECT dayofmonth(s.anls_cmpl_date) AS c1, COUNT(*) AS c2"
                + " FROM sample s INNER JOIN bundle b ON s.bundle_id = b.id AND b.is_active = 1 "
                + " WHERE s.anls_cmpl_date BETWEEN :sDate AND :fDate "
                + " AND s.is_test = 0 "
                + " AND s.is_last_version = 1 "
                + " AND s.bundle_id in :bundleIds "
                + " AND s.status_code in :statusCode "
                + " GROUP BY dayofmonth(s.anls_cmpl_date) ", nativeQuery = true)
    List<String[]> findCalendarDataByAnlsCmpldDate(@Param("sDate") String sDate, @Param("fDate") String fDate
                                            , @Param("bundleIds") List<Integer> bundleIds
                                            , @Param("statusCode") List<String> statusCodes);

    @Query(value = "SELECT dayofmonth(s.output_cmpl_date) AS c1, COUNT(*) AS c2"
                + " FROM sample s INNER JOIN bundle b ON s.bundle_id = b.id AND b.is_active = 1 "
                + " WHERE s.output_cmpl_date BETWEEN :sDate AND :fDate "
                + " AND s.is_test = 0 "
                + " AND s.is_last_version = 1 "
                + " AND s.bundle_id in :bundleIds "
                + " AND s.status_code in :statusCode "
                + " GROUP BY dayofmonth(s.output_cmpl_date) ", nativeQuery = true)
    List<String[]> findCalendarDataByOutputCmplDate(@Param("sDate") String sDate, @Param("fDate") String fDate
                                            , @Param("bundleIds") List<Integer> bundleIds
                                            , @Param("statusCode") List<String> statusCodes);
    
    @Query(value = "SELECT DATE_FORMAT(s.output_cmpl_date, '%Y%m') AS yyyymm, CONVERT(COUNT(*), char) AS count "
    			+ " , b.name AS name "
                + " FROM sample s INNER JOIN bundle b ON s.bundle_id = b.id AND b.is_active = 1 "
                + " WHERE s.anls_cmpl_date BETWEEN :sDate AND :fDate "
                + " AND s.is_test = 0 "
                + " AND s.is_last_version = 1 "
                + " AND s.bundle_id in :bundleIds "
                + " AND s.status_code in :statusCode "
                + " AND s.output_cmpl_date IS NOT NULL "
                + " GROUP BY EXTRACT(YEAR_MONTH FROM s.created_date), s.bundle_id ", nativeQuery = true)
    List<Map<String, String>> findChartDataByCreatedDate(@Param("sDate") String sDate
											, @Param("fDate") String fDate
                                            , @Param("bundleIds") List<Integer> bundleIds
                                            , @Param("statusCode") List<String> statusCodes);
    
    @Query(value = "SELECT DATE_FORMAT(s.output_cmpl_date, '%Y%m') AS yyyymm, CONVERT(COUNT(*), char) AS count "
			+ " , b.name AS name "
                + " FROM sample s INNER JOIN bundle b ON s.bundle_id = b.id AND b.is_active = 1 "
                + " WHERE s.anls_cmpl_date BETWEEN :sDate AND :fDate "
                + " AND s.is_test = 0 "
                + " AND s.is_last_version = 1 "
                + " AND s.bundle_id in :bundleIds "
                + " AND s.status_code in :statusCode "
                + " AND s.output_cmpl_date IS NOT NULL "
                + " GROUP BY EXTRACT(YEAR_MONTH FROM s.created_date), s.bundle_id ", nativeQuery = true)
    List<Map<String, String>> findChartDataByAnlsCmplDate(@Param("sDate") String sDate
    										, @Param("fDate") String fDate
                                            , @Param("bundleIds") List<Integer> bundleIds
                                            , @Param("statusCode") List<String> statusCodes);
    
    @Query(value = "SELECT DATE_FORMAT(s.output_cmpl_date, '%Y%m') AS yyyymm, CONVERT(COUNT(*), char) AS count "
			+ " , b.name AS name "
                + " FROM sample s INNER JOIN bundle b ON s.bundle_id = b.id AND b.is_active = 1 "
                + " WHERE s.output_cmpl_date BETWEEN :sDate AND :fDate "
                + " AND s.is_test = 0 "
                + " AND s.is_last_version = 1 "
                + " AND s.bundle_id in :bundleIds "
                + " AND s.status_code in :statusCode "
                + " AND s.output_cmpl_date IS NOT NULL "
                + " GROUP BY EXTRACT(YEAR_MONTH FROM s.output_cmpl_date), s.bundle_id ", nativeQuery = true)
    List<Map<String, String>> findChartDataByOutputCmplDate(@Param("sDate") String sDate
											, @Param("fDate") String fDate
                                            , @Param("bundleIds") List<Integer> bundleIds
                                            , @Param("statusCode") List<String> statusCodes);

    @Query(value = "SELECT MAX(SUBSTR(laboratory_id, -4, 4)) FROM sample WHERE is_test = 1 AND bundle_id = :bundleId", nativeQuery = true)
    String findMaxTestLaboratoryId(@Param("bundleId") int bundleId);

    @Query(value = "SELECT MAX(SUBSTR(management_number, -7, 7)) FROM sample WHERE is_test = 1 AND bundle_id = :bundleId", nativeQuery = true)
    String findMaxTestManagementNumber(@Param("bundleId") int bundleId);
}
