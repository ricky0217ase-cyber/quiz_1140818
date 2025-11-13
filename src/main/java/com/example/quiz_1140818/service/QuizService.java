package com.example.quiz_1140818.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.quiz_1140818.constants.QuestionType;
import com.example.quiz_1140818.constants.ResCodeMessage;
import com.example.quiz_1140818.dao.QuestionDao;
import com.example.quiz_1140818.dao.QuizDao;
import com.example.quiz_1140818.entity.Question;
import com.example.quiz_1140818.entity.Quiz;
import com.example.quiz_1140818.response.BasicRes;
import com.example.quiz_1140818.response.QuestionListRes;
import com.example.quiz_1140818.response.QuizListRes;
import com.example.quiz_1140818.vo.Options;
import com.example.quiz_1140818.vo.QuestionVo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class QuizService {

	private ObjectMapper mapper = new ObjectMapper();

	@Autowired
	private QuizDao quizDao;

	@Autowired
	private QuestionDao questionDao;

	/**
	 * @Transactional: 1. 使用的於修改資料時(insert/update/delete)且有以下2種情形<br>
	 *                 1.1 同一個方法中有使用多個 Dao 時，例如下面方法中有同時使用 quizDao 和 questionDao<br>
	 *                 1.2 同一個 Dao 有修改多筆的資料，例如下面方法中有使用 questionDao 新增多筆的問題<br>
	 *                 rollbackFor = Exception.class <br>
	 *                 2. 其預設的有效作用範圍是當程式發生 RuntimeException(以及其子類別) 時才會讓資料回朔，所以為了
	 *                 在發生其他 Exception 時也可以讓資料回朔，就要把作用範圍提升到所有例外的父類別: Exception<br>
	 *                 3. 要讓 @Transactional 有效的另一個條件必須要把發生的 Exception 給它 throw 出去
	 */
	@Transactional(rollbackFor = Exception.class)
	public BasicRes create(Quiz quiz, List<QuestionVo> questionVoList) throws Exception {

		try {
			// 檢查 question
			BasicRes checkRes = checkQuestion(questionVoList);
			if (checkRes != null) { // checkRes != null 檢查結果有誤
				return checkRes;
			}
			// 檢查問卷時間 : 開始時間不能比結束時間晚
			checkRes = checkDate(quiz.getStartDate(), quiz.getEndDate());
			if (checkRes != null) { // checkRes != null 檢查結果有誤
				return checkRes;
			}
			// 新增 quiz
			quizDao.create(quiz.getTitle(), quiz.getDescription(), //
					quiz.getStartDate(), quiz.getEndDate(), quiz.isPublish());
			// 取 quiz_id，因為 quiz_id 是流水號，要等資料新增進 DB 後才會自動產生
			int quizId = quizDao.selectMaxId();
			// 將 QuestionVo 中的 List<Options> 轉成字串
			// 處理 Question
			// 把 vo 轉成 Question entity 後，再將資料寫進 DB 中
			for (QuestionVo vo : questionVoList) {
				List<Options> optionsList = vo.getOptionsList();
				if (optionsList == null) {
					optionsList = new ArrayList<>();
				}
				String optionsStr = mapper.writeValueAsString(vo.getOptionsList());
				// 新增 question
				questionDao.create(quizId, vo.getQuestionId(), //
						vo.getName(), optionsStr, vo.getType(), vo.isRequired());
			}

		} catch (Exception e) {
			throw e;
		}

		return new BasicRes(ResCodeMessage.SUCCESS.getCode(), //
				ResCodeMessage.SUCCESS.getMessage());
	}

	// 檢查 question 的 type 以及選項
	private BasicRes checkQuestion(List<QuestionVo> questionVoList) {
		for (QuestionVo item : questionVoList) {
			if (!QuestionType.checkAllType(item.getType())) {
				// 檢查的結果是 false 表示 question 中的 type 不是設定的3種型態之一
				return new BasicRes(ResCodeMessage.QUESTION_TYPE_ERROR.getCode(), //
						ResCodeMessage.QUESTION_TYPE_ERROR.getMessage());
			}
			// 檢查選項 :
			// 正確的應該是 : type 是 1.簡答題時不會有選項 或 2.選擇題時要有選項
			// 以下是排除 : 當 type 是簡答題時卻有選項
			if (!QuestionType.checkChoiceType(item.getType())) {
				if (!item.getOptionsList().isEmpty()) {
					return new BasicRes(ResCodeMessage.QUESTION_TYPE_OPTIONS_MISMATCH.getCode(), //
							ResCodeMessage.QUESTION_TYPE_OPTIONS_MISMATCH.getMessage());
				}
			}
			// 以下是排除 : 當 type 是單或多選題時卻沒有選項
			if (QuestionType.checkChoiceType(item.getType())) {
				if (item.getOptionsList().isEmpty()) {
					return new BasicRes(ResCodeMessage.QUESTION_TYPE_OPTIONS_MISMATCH.getCode(), //
							ResCodeMessage.QUESTION_TYPE_OPTIONS_MISMATCH.getMessage());
				}

			}
		}
		return null;
	}

	/**
	 * 檢查問卷日期<br>
	 * 1. 開始日期不能比結束日期晚 或 結束日期不能比開始日期早<br>
	 * 2. 條件1 成立下，開始日期不能比當前日期早<br>
	 * 3. startDate.isAfter(endDate): <br>
	 * 3.1 startDate 早於 endDate --> false <br>
	 * 3.2 startDate 等於 endDate --> false <br>
	 * 3.3 startDate 晚於 endDate --> true
	 */
	private BasicRes checkDate(LocalDate startDate, LocalDate endDate) {
		// 正常來說，問卷的開始日期一定是等於或晚於結束日期，若 startDate.isAfter(endDate)
		// 的結果是 true，則表示問卷的兩個時間是錯的
		if (startDate.isAfter(endDate)) {
			return new BasicRes(ResCodeMessage.QUIZ_DATE_ERROR.getCode(), //
					ResCodeMessage.QUIZ_DATE_ERROR.getMessage());
		}
		// isBefore 也是不包含兩個日期相等
		if (startDate.isBefore(LocalDate.now())) {
			return new BasicRes(ResCodeMessage.QUIZ_DATE_ERROR.getCode(), //
					ResCodeMessage.QUIZ_DATE_ERROR.getMessage());
		}
		return null;
	}

	@Transactional
	public BasicRes update(Quiz quiz, List<QuestionVo> questionVoList) throws Exception {
		try {
			// 檢查 quizId 是否存在於 DB
			int quizId = quiz.getId();
			// 搜尋欄位 Id 出現的次數，因為 id 是 PK ，所以結果只會出現 0 或 1
			if (quizDao.selectCountId(quizId) == 0) { // 0 表示該 quizId 不存在
				return new BasicRes(ResCodeMessage.NOT_FOUND.getCode(), //
						ResCodeMessage.NOT_FOUND.getMessage());
			}

			BasicRes checkRes = checkDate(quiz.getStartDate(), quiz.getEndDate());
			if (checkRes != null) { // checkRes != null 檢查結果有誤
				return checkRes;
			}
			// 更新 quiz
			quizDao.update(quizId, quiz.getTitle(), quiz.getDescription(), //
					quiz.getStartDate(), quiz.getEndDate(), quiz.isPublish());

			// 更新 question
			// 刪除相同 quizId 的所有問卷
			questionDao.deleteByQuizId(quizId);

			// 把 vo 轉成 Question entity 後，再將資料寫進 DB 中
			for (QuestionVo vo : questionVoList) {
				List<Options> optionsList = vo.getOptionsList();
				if (optionsList == null) {
					optionsList = new ArrayList<>();
				}
				String optionsStr = mapper.writeValueAsString(vo.getOptionsList());
				// 新增 question
				// 確保這些問題都是同一個 quizId --> 使用
				questionDao.create(quizId, vo.getQuestionId(), //
						vo.getName(), optionsStr, vo.getType(), vo.isRequired());
			}

			return new BasicRes(ResCodeMessage.SUCCESS.getCode(), //
					ResCodeMessage.SUCCESS.getMessage());
		} catch (Exception e) {
			throw e;
		}

	}

	public QuizListRes getQuizList() {
		return new QuizListRes(ResCodeMessage.SUCCESS.getCode(), //
				ResCodeMessage.SUCCESS.getMessage(), quizDao.getAll());
	}
	
	public QuizListRes getQuizList(boolean getPublish) {
		if(getPublish) {
			return new QuizListRes(ResCodeMessage.SUCCESS.getCode(), //
					ResCodeMessage.SUCCESS.getMessage(), quizDao.getPublishedAll());
		}
		return new QuizListRes(ResCodeMessage.SUCCESS.getCode(), //
				ResCodeMessage.SUCCESS.getMessage(), quizDao.getAll());
	}

	public QuizListRes getQuizList(String title, LocalDate startDate, LocalDate endDate, boolean getPublish) {

		// 假設 startDate 和 endDate --> 檢查 endDate > startDate
		if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
			return new QuizListRes(ResCodeMessage.QUIZ_DATE_ERROR.getCode(), //
					ResCodeMessage.QUIZ_DATE_ERROR.getMessage(), quizDao.getAll());
		}
		
		// 若 title 沒帶值(預設是 null )或 空字串 或 全空白字串 --> 一律轉換成空字串
		// 到時 SQL 中搭配 like %空字串的title% 就把所有 title 的資料撈出來
		if (!StringUtils.hasText(title)) {
			title = "";
		}

		// 轉換沒有帶值的開始日期
		if (startDate == null) {
			startDate = LocalDate.of(1970, 1, 1); // 很早之前的時間
		}

		// 轉換沒有帶值的結束日期
		if (endDate == null) {
			endDate = LocalDate.of(2999, 12, 31); // 很久之後的時間
		}

		if(getPublish) {
			return new QuizListRes(ResCodeMessage.SUCCESS.getCode(), //
					ResCodeMessage.SUCCESS.getMessage(), quizDao.getPublishedSearch(title, startDate, endDate));
		}
		
		return new QuizListRes(ResCodeMessage.SUCCESS.getCode(), //
				ResCodeMessage.SUCCESS.getMessage(), quizDao.getSearch(title, startDate, endDate));
	}

	public QuestionListRes getQuestionList(int quizId) throws Exception {
		//
		if (quizId <= 0) {
			return new QuestionListRes(ResCodeMessage.QUIZ_ID_ERROR.getCode(), //
					ResCodeMessage.QUIZ_ID_ERROR.getMessage());
		}
		Quiz quiz = quizDao.getById(quizId);
		// 搜尋欄位 Id 出現的次數，因為 id 是 PK ，所以結果只會出現 0 或 1
		if (quizDao.selectCountId(quizId) == 0) { // 0 表示該 quizId 不存在
			return new QuestionListRes(ResCodeMessage.NOT_FOUND.getCode(), //
					ResCodeMessage.NOT_FOUND.getMessage());
		}

		// 用 quizId 從 question 表取資料
		List<Question> questionList = questionDao.getByQuizId(quizId);
		// 建立 List<QuestionVo> 用來放下面 for 迴圈中所建立的每個 QuestionVo
		List<QuestionVo> voList = new ArrayList<>();
		// 將每個 optionsStr 轉成 List<Options>
		for (Question item : questionList) {
			List<Options> optionsList = null;
			try {
				optionsList = mapper.readValue(item.getOptionsStr(), new TypeReference<>() {

				});
			} catch (Exception e) {
				throw e;
			}

			// 把每個 Question 的屬性值塞到 QuestionVo 中
			QuestionVo vo = new QuestionVo(item.getQuizId(), item.getQuestionId(), //
					item.getName(), optionsList, item.getType(), item.isRequired());

			voList.add(vo);
		}
		return new QuestionListRes(ResCodeMessage.SUCCESS.getCode(), //
				ResCodeMessage.SUCCESS.getMessage(),//
				quiz.getTitle(),quiz.getStartDate(),quiz.getEndDate(),quiz.getDescription(),voList);
	}
	
	@Transactional(rollbackFor = Exception.class)
	public BasicRes deleteByQuizId(List<Integer> quizIdList) {
		try {
			// 刪 quiz
			quizDao.deleteByIdWithCondition(quizIdList);
			// 刪 question
			questionDao.deleteByQuizIdIn(quizIdList);
		} catch (Exception e) {
			throw e;
		}
		
		return new BasicRes(ResCodeMessage.SUCCESS.getCode(), //
				ResCodeMessage.SUCCESS.getMessage());
	}
}
