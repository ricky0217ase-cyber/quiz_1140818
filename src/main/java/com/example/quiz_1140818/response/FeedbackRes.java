package com.example.quiz_1140818.response;

import java.util.List;

import com.example.quiz_1140818.vo.FeedbackVo;

public class FeedbackRes extends BasicRes{

	private List<FeedbackVo> feedbackVoList;

	public FeedbackRes() {
		super();
		// TODO Auto-generated constructor stub
	}

	public FeedbackRes(int code, String message) {
		super(code, message);
		// TODO Auto-generated constructor stub
	}

	public FeedbackRes(int code, String message, List<FeedbackVo> feedbackVoList) {
		super(code, message);
		this.feedbackVoList = feedbackVoList;
	}

	public List<FeedbackVo> getFeedbackVoList() {
		return feedbackVoList;
	}

	public void setFeedbackVoList(List<FeedbackVo> feedbackVoList) {
		this.feedbackVoList = feedbackVoList;
	}
	
}
