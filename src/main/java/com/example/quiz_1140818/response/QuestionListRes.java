package com.example.quiz_1140818.response;

import java.time.LocalDate;
import java.util.List;

import com.example.quiz_1140818.vo.QuestionVo;

public class QuestionListRes extends BasicRes {

	private String title;

	private LocalDate startDate;

	private LocalDate endDate;

	private String description;

	private List<QuestionVo> questionVoList;

	public QuestionListRes() {
		super();
	}

	public QuestionListRes(int code, String message) {
		super(code, message);
	}

	public QuestionListRes(int code, String message, String title, LocalDate startDate, LocalDate endDate,
			String description, List<QuestionVo> questionVoList) {
		super(code, message);
		this.title = title;
		this.startDate = startDate;
		this.endDate = endDate;
		this.description = description;
		this.questionVoList = questionVoList;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

	public void setStartDate(LocalDate startDate) {
		this.startDate = startDate;
	}

	public LocalDate getEndDate() {
		return endDate;
	}

	public void setEndDate(LocalDate endDate) {
		this.endDate = endDate;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<QuestionVo> getQuestionVoList() {
		return questionVoList;
	}

	public void setQuestionVoList(List<QuestionVo> questionVoList) {
		this.questionVoList = questionVoList;
	}

}
