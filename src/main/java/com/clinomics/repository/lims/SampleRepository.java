package com.clinomics.repository.lims;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.clinomics.entity.lims.Bundle;
import com.clinomics.entity.lims.Sample;
import com.clinomics.enums.StatusCode;

public interface SampleRepository extends JpaRepository<Sample, Integer>, JpaSpecificationExecutor<Sample> {
    List<Sample> findByIdIn(List<Integer> id);
    List<Sample> findByIdInAndStatusCodeIn(List<Integer> id, List<StatusCode> statusCodes);
    //Optional<Sample> findTopByBundle_IdOrderByLaboratoryIdDesc(int bundleId);
    //Optional<Sample> findTopByBundle_IdAndReceivedDateOrderByLaboratoryIdDesc(int bundleId, LocalDate receivedDate);
    
	@Query(value = "SELECT MAX(laboratory_id) FROM sample WHERE bundle_id = ?1", nativeQuery = true)
	String findMaxLaboratoryId(int bundleId);
	@Query(value = "SELECT MAX(laboratory_id) FROM sample WHERE bundle_id = ?1 AND DATE_FORMAT(received_date, '%Y%m') = ?2", nativeQuery = true)
	String findMaxHospitalLaboratoryId(int bundleId, String yyyymm);
    @Query(value = "SELECT DISTINCT REPLACE(JSON_EXTRACT(items, '$.h_name'), '\"', '') AS name FROM sample WHERE JSON_EXTRACT(items, '$.h_name') IS NOT NULL", nativeQuery = true)
    List<String> findDistinctHospitalName();

    @Query(value = "SELECT dayofmonth(s.created_date) AS c1, COUNT(*) AS c2"
                + " , SUM(CASE WHEN s.output_cmpl_date IS NULL THEN 0 ELSE 1 END) AS c3"
                + " FROM sample s INNER JOIN bundle b ON s.bundle_id = b.id AND b.is_active = 1 "
                + " WHERE EXTRACT(YEAR_MONTH FROM s.created_date) = :yyyymm "
                + " AND s.is_last_version = 1 "
                + " AND s.bundle_id in :bundleIds "
                + " AND s.status_code in :statusCode "
                + " GROUP BY dayofmonth(s.created_date) ", nativeQuery = true)
    List<String[]> findCalendarDataByCreatedDate(@Param("yyyymm") String yyyymm
                                            , @Param("bundleIds") List<Integer> bundleIds
                                            , @Param("statusCode") List<String> statusCodes);

    @Query(value = "SELECT dayofmonth(s.modified_date) AS c1, COUNT(*) AS c2"
                + " FROM sample s INNER JOIN bundle b ON s.bundle_id = b.id AND b.is_active = 1 "
                + " WHERE EXTRACT(YEAR_MONTH FROM s.modified_date) = :yyyymm "
                + " AND s.is_last_version = 1 "
                + " AND s.bundle_id in :bundleIds "
                + " AND s.status_code in :statusCode "
                + " GROUP BY dayofmonth(s.modified_date) ", nativeQuery = true)
    List<String[]> findCalendarDataByModifiedDate(@Param("yyyymm") String yyyymm
                                            , @Param("bundleIds") List<Integer> bundleIds
                                            , @Param("statusCode") List<String> statusCodes);

    @Query(value = "SELECT dayofmonth(s.output_cmpl_date) AS c1, COUNT(*) AS c2"
                + " FROM sample s INNER JOIN bundle b ON s.bundle_id = b.id AND b.is_active = 1 "
                + " WHERE EXTRACT(YEAR_MONTH FROM s.output_cmpl_date) = :yyyymm "
                + " AND s.is_last_version = 1 "
                + " AND s.bundle_id in :bundleIds "
                + " AND s.status_code in :statusCode "
                + " GROUP BY dayofmonth(s.output_cmpl_date) ", nativeQuery = true)
    List<String[]> findCalendarDataByOutputCmplDate(@Param("yyyymm") String yyyymm
                                            , @Param("bundleIds") List<Integer> bundleIds
                                            , @Param("statusCode") List<String> statusCodes);
    
    @Query(value = "SELECT DATE_FORMAT(s.output_cmpl_date, '%Y%m') AS yyyymm, CONVERT(COUNT(*), char) AS count "
    			+ " , b.name AS name "
                + " FROM sample s INNER JOIN bundle b ON s.bundle_id = b.id AND b.is_active = 1 "
                + " WHERE EXTRACT(YEAR_MONTH FROM s.anls_cmpl_date) BETWEEN :sDate AND :fDate "
                + " AND s.is_last_version = 1 "
                + " AND s.bundle_id in :bundleIds "
                + " AND s.status_code in :statusCode "
                + " GROUP BY EXTRACT(YEAR_MONTH FROM s.created_date), s.bundle_id ", nativeQuery = true)
    List<Map<String, String>> findChartDataByCreatedDate(@Param("sDate") String sDate
											, @Param("fDate") String fDate
                                            , @Param("bundleIds") List<Integer> bundleIds
                                            , @Param("statusCode") List<String> statusCodes);
    
    @Query(value = "SELECT DATE_FORMAT(s.output_cmpl_date, '%Y%m') AS yyyymm, CONVERT(COUNT(*), char) AS count "
			+ " , b.name AS name "
                + " FROM sample s INNER JOIN bundle b ON s.bundle_id = b.id AND b.is_active = 1 "
                + " WHERE EXTRACT(YEAR_MONTH FROM s.anls_cmpl_date) BETWEEN :sDate AND :fDate "
                + " AND s.is_last_version = 1 "
                + " AND s.bundle_id in :bundleIds "
                + " AND s.status_code in :statusCode "
                + " GROUP BY EXTRACT(YEAR_MONTH FROM s.created_date), s.bundle_id ", nativeQuery = true)
    List<Map<String, String>> findChartDataByAnlsCmplDate(@Param("sDate") String sDate
    										, @Param("fDate") String fDate
                                            , @Param("bundleIds") List<Integer> bundleIds
                                            , @Param("statusCode") List<String> statusCodes);
    
    @Query(value = "SELECT DATE_FORMAT(s.output_cmpl_date, '%Y%m') AS yyyymm, CONVERT(COUNT(*), char) AS count "
			+ " , b.name AS name "
                + " FROM sample s INNER JOIN bundle b ON s.bundle_id = b.id AND b.is_active = 1 "
                + " WHERE EXTRACT(YEAR_MONTH FROM s.output_cmpl_date) BETWEEN :sDate AND :fDate "
                + " AND s.is_last_version = 1 "
                + " AND s.bundle_id in :bundleIds "
                + " AND s.status_code in :statusCode "
                + " GROUP BY EXTRACT(YEAR_MONTH FROM s.output_cmpl_date), s.bundle_id ", nativeQuery = true)
    List<Map<String, String>> findChartDataByOutputCmplDate(@Param("sDate") String sDate
											, @Param("fDate") String fDate
                                            , @Param("bundleIds") List<Integer> bundleIds
                                            , @Param("statusCode") List<String> statusCodes);
}
