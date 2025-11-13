package com.example.quiz_1140818.dao;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.example.quiz_1140818.entity.Quiz;

@Repository
public interface QuizDao extends JpaRepository<Quiz, Integer> {

	@Modifying
	@Transactional
	@Query(value = "insert into quiz (title, description, start_date, end_date,"//
			+ " is_publish) values(?1, ?2, ?3, ?4, ?5)", nativeQuery = true)
	public void create(String title, String description, //
			LocalDate startDate, LocalDate endDate, boolean publish);
	
	// 找尋欄位 id 當前的最大值，因為 id 是流水號，最大值也表示最新新增的一筆資料
	@Query(value = "select max(id) from quiz", nativeQuery = true)
	public int selectMaxId();
	
	// select count(id) 是去搜尋欄位 id 出現的次數
	@Query(value = "select count(id) from quiz where id = ?1", nativeQuery = true)
	public int selectCountId(int id);
	
	@Modifying
	@Transactional
	@Query(value = "update quiz set title = ?2, description = ?3," //
			+ " start_date = ?4, end_date = ?5, is_publish = ?6 where id = ?1", nativeQuery = true)
	public int update(int id, String title, String description,//
			LocalDate startDate, LocalDate endDate, boolean publish);
	
	@Query(value = "select * from quiz ", nativeQuery = true)
	public List<Quiz> getAll();
	
	@Query(value = "select * from quiz where ia_publish is true", nativeQuery = true)
	public List<Quiz> getPublishedAll();
	
	@Query(value = "select * from quiz where title like %?1% and" //
			+ " start_date >= ?2 and end_date <= ?3", nativeQuery = true)
	public List<Quiz> getSearch(String title , LocalDate startDate, LocalDate endDate);
	
	@Query(value = "select * from quiz where title like %?1% and" //
			+ " start_date >= ?2 and end_date <= ?3 and is_publish is true", nativeQuery = true)
	public List<Quiz> getPublishedSearch(String title , LocalDate startDate, LocalDate endDate);
	
	// 只刪除 未發布 、 已發布且已結束
	@Modifying
	@Transactional
	@Query(value = "delete from quiz where "
	        + " (not is_publish or (is_publish = 1 and now() > end_date)) "
	        + " and id in (?1) "
			, nativeQuery = true)
	public void deleteByIdWithCondition(List<Integer> quizIdList);
	
	@Query(value = "select * from quiz where id = ?1", nativeQuery = true)
	public Quiz getById(int id);
	
}
