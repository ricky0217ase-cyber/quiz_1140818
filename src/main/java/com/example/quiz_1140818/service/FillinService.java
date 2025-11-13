package com.example.quiz_1140818.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.example.quiz_1140818.constants.QuestionType;
import com.example.quiz_1140818.constants.ResCodeMessage;
import com.example.quiz_1140818.dao.FillinDao;
import com.example.quiz_1140818.dao.QuestionDao;
import com.example.quiz_1140818.dao.QuizDao;
import com.example.quiz_1140818.entity.Fillin;
import com.example.quiz_1140818.entity.Question;
import com.example.quiz_1140818.entity.Quiz;
import com.example.quiz_1140818.entity.User;
import com.example.quiz_1140818.response.BasicRes;
import com.example.quiz_1140818.response.FeedbackRes;
import com.example.quiz_1140818.response.StatisticRes;
import com.example.quiz_1140818.vo.Answer;
import com.example.quiz_1140818.vo.FeedbackVo;
import com.example.quiz_1140818.vo.Options;
import com.example.quiz_1140818.vo.OptionsCount;
import com.example.quiz_1140818.vo.QuestionAnswerVo;
import com.example.quiz_1140818.vo.QuestionCountVo;
import com.example.quiz_1140818.vo.StatisticVo;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class FillinService {
	
	private ObjectMapper mapper = new ObjectMapper();

	@Autowired
	private FillinDao fillinDao;

	@Autowired
	private QuizDao quizDao;

	@Autowired
	private QuestionDao questionDao;

	@Transactional(rollbackFor = Exception.class)
	public BasicRes fillin(User user, int quizId, List<Answer> answerList) throws Exception {

		// 要檢查答案，檢查的基準就是要存在 DB 中的同張問卷的那些問題
		// 取出同張問卷的所有問題
		List<Question> questionList = questionDao.getByQuizId(quizId);
		for (Question question : questionList) {
			// 檢查必田是否有答案
			if (question.isRequired()) {
				BasicRes res = checkRequiredAnswer(question.getQuestionId(), answerList, question.getType());
				if (res != null) {
					return res;
				}
			}
			// 相同問題編號下，答案中的選項是否跟問卷中的一樣
			List<Options> reqOptionList = new ArrayList<>();
			for (Answer answer : answerList) {
				if (question.getQuestionId() == answer.getQuestionId()) {
					reqOptionList = answer.getOptionsList();
					break;
				}
			}
			try {
				BasicRes res = checkOptions(question.getOptionsStr(), reqOptionList);
				if (res != null) {
					return res;
				}
			} catch (Exception e) {
				throw e;
			}

		}
		// 寫答案
		try {
			for (Answer item : answerList) {
				fillinDao.fillin(quizId, item.getQuestionId(), user.getEmail(), //
						user.getName(), user.getPhone(), user.getAge(), user.getGender(), //
						mapper.writeValueAsString(item), LocalDate.now());
			}
		} catch (Exception e) {
			throw e;
		}

		return new BasicRes(ResCodeMessage.SUCCESS.getCode(), //
				ResCodeMessage.SUCCESS.getMessage());
	}

	private BasicRes checkRequiredAnswer(int questionId, List<Answer> answerList, String type) {
		for (Answer answer : answerList) {
			// 比對相同問題編號，確認問題型態所對應的 answer 是否有值
			if (answer.getQuestionId() == questionId) {
				if (type.equalsIgnoreCase(QuestionType.SINGLE.getType())) {
					if (answer.getRadioAnswer() <= 0) {
						return new BasicRes(ResCodeMessage.RADIO_ANSWER_IS_REQUIRED.getCode(), //
								ResCodeMessage.RADIO_ANSWER_IS_REQUIRED.getMessage());
					}
				} else if (type.equalsIgnoreCase(QuestionType.TEXT.getType())) {
					if (!StringUtils.hasText(answer.getTextAnswer())) {
						return new BasicRes(ResCodeMessage.TEXT_ANSWER_IS_REQUIRED.getCode(), //
								ResCodeMessage.TEXT_ANSWER_IS_REQUIRED.getMessage());
					}
				} else {
					for (Options item : answer.getOptionsList()) {
						// 檢查至少有一個 checkBoolean 的值是 true
						if (item.isCheckBoolean()) {
							return null;
						}
						return new BasicRes(ResCodeMessage.CHECKBOX_ANSWER_IS_REQUIRED.getCode(), //
								ResCodeMessage.CHECKBOX_ANSWER_IS_REQUIRED.getMessage());
					}
				}
			}
		}
		return null;
	}

	private BasicRes checkOptions(String optionsStr, List<Options> reqOptionsList) throws Exception {
		// 轉換 optionsStr 成物件 List<Options>
		try {
			List<Options> optionsList = mapper.readValue(optionsStr, new TypeReference<>() {
			});
			for (Options item : optionsList) {
				int code = item.getCode();
				String optionsName = item.getOptionsName();
				for (Options reqItem : reqOptionsList) {
					// 相同編號下，若選項不一樣，則回傳錯誤
					if (code == reqItem.getCode()) {
						if (!optionsName.equalsIgnoreCase(reqItem.getOptionsName())) {
							return new BasicRes(ResCodeMessage.QUESTION_OPTION_MISMATCH.getCode(), //
									ResCodeMessage.QUESTION_OPTION_MISMATCH.getMessage());
						}
					}
				}
			}
		} catch (Exception e) {
			throw e;
		}
		return null;
	}

	public FeedbackRes feedback(int quizId) throws Exception {
		if (quizId <= 0) {
			return new FeedbackRes(ResCodeMessage.QUIZ_ID_ERROR.getCode(), //
					ResCodeMessage.QUIZ_ID_ERROR.getMessage());
		}
		List<Question> questionList = questionDao.getByQuizId(quizId);
		// 將 Question 轉成 QuestionAnswerVo
		Map<Integer, QuestionAnswerVo> map = new HashMap<>();
		for (Question question : questionList) {
			QuestionAnswerVo vo = new QuestionAnswerVo(question.getQuizId(), //
					question.getQuestionId(), question.getName(), //
					question.getType(), question.isRequired());
			// 問題編號，vo
			map.put(question.getQuestionId(), vo);
		}
		Quiz quiz = quizDao.getById(quizId);

		List<Fillin> fillinList = fillinDao.getByQuizId(quizId);
		// ====================================
		// 一個 email 表示一位使用者的 FeedbackVo
		Map<String, FeedbackVo> emailFeedbackVoMap = new HashMap<>();
		for (Fillin item : fillinList) {
			FeedbackVo feedbackVo = new FeedbackVo();
			List<QuestionAnswerVo> questionAnswerVoList = new ArrayList<>();
			String email = item.getEmail();
			if (!emailFeedbackVoMap.containsKey(email)) { // 表示尚未記錄到該 user 的答案
				User user = new User(item.getName(), item.getPhone(), item.getEmail(), //
						item.getAge(), item.getGender());
				// 將 User、Quiz、QuestionVoList、FillinDate 設定到 feedbackVo
				feedbackVo.setUser(user);
				feedbackVo.setQuiz(quiz);
				feedbackVo.setFillinDate(item.getFillinDate());
				feedbackVo.setQuestionVoList(questionAnswerVoList);
				emailFeedbackVoMap.put(email, feedbackVo);
			} else {
				feedbackVo = emailFeedbackVoMap.get(email);
				questionAnswerVoList = feedbackVo.getQuestionVoList();
			}

			try {
				Answer ans = mapper.readValue(item.getAnswerStr(), Answer.class);
				// 透過 questionId 當作 key 從 map 中取得對應的 QuestionAnswerVo
				QuestionAnswerVo vo = map.get(item.getQuestionId());
				vo.setOptionsList(ans.getOptionsList());
				vo.setTextAnswer(ans.getTextAnswer());
				vo.setRadioAnswer(ans.getRadioAnswer());
				questionAnswerVoList.add(vo);
			} catch (Exception e) {
				throw e;
			}

		}
		// 將 emailFeedbackVoMap 的 FeedbackVo 增加到 feedbackVoList
		List<FeedbackVo> feedbackVoList = new ArrayList<>();
		for (Entry<String, FeedbackVo> mapItem : emailFeedbackVoMap.entrySet()) {
			feedbackVoList.add(mapItem.getValue());
		}
		return new FeedbackRes(ResCodeMessage.SUCCESS.getCode(), //
				ResCodeMessage.SUCCESS.getMessage(), feedbackVoList);
	}

//	public StatisticRes statistic(int quizId) throws Exception {
//		if (quizId <= 0) {
//			return new StatisticRes(ResCodeMessage.QUIZ_ID_ERROR.getCode(), //
//					ResCodeMessage.QUIZ_ID_ERROR.getMessage());
//		}
//		// 將問題相關的資訊設定給 QuestionAnswerVo
//		// questionId, QuestionCountVo
//		Map<Integer, QuestionCountVo> voMap = setQuestionAnswerVo(quizId);
//		// 使用 QuizId 撈取所有的填答
//		List<Fillin> fillinList = fillinDao.getByQuizId(quizId);
//		// 問題編號, 選項編號 選項 次數
//		Map<Integer, Map<Integer, Map<String, Integer>>> map = new HashMap<>();
//		for (Fillin fillin : fillinList) {
//			try {
//				// 1. 把 answer_str 轉成 Answer
//				Answer ans = mapper.readValue(fillin.getAnswerStr(), Answer.class);
//				// 2. 統計次數
//				// 2.1 簡答題
//				if (StringUtils.hasText(ans.getTextAnswer())) {
//					// textAnswer 有內容的話，表示該題是簡答題 --> 跳過
//					continue;
//				}
//				// 選項編號, 選項, 次數
//				Map<Integer, Map<String, Integer>> codeOpCountMap = new HashMap<>();
//				if (map.containsKey(ans.getQuestionId())) {
//					// 若問題編號已存在，則把對應的 選項編號、選項、次數的 Map 取出
//					codeOpCountMap = map.get(ans.getQuestionId());
//				}
//				// 2.2 多選題: 先做的原因是因為要先取得選項編號與選項，而其答案是綁定在 List<Options> 中
//				// 可以順便蒐集次數
//				for (Options op : ans.getOptionsList()) {
//					// 先判斷 opCountMap 中是否已有蒐集過的選項編號
//					if (codeOpCountMap.containsKey(op.getCode())) {
//						// 有蒐集過的選項編號
//						// 判斷 checkBoolean 的值是否為 true
//						if (op.isCheckBoolean()) {
//							// --> 取出對應的 value (選項和次數的 map)
//							// 選項, 次數
//							Map<String, Integer> opCountMap = codeOpCountMap.get(op.getCode());
//							// --> 取出選項對應的次數後再 + 1
//							int count = opCountMap.get(op.getOptionsName()) + 1;
//							// --> 將更新後的次數放回(put) opCountMap
//							opCountMap.put(op.getOptionsName(), count);
//							// codeOpCountMap 不需要更新，因為其對應 value 的記憶體上的值(opCountMap)已更新
//						}
//					} else {
//						// 沒有蒐集過的選項編號 --> 建立新的, 次數是 0
//						Map<String, Integer> opCountMap = new HashMap<>();
//						int count = 0;
//						// checkBoolean 的值是否為 true
//						if (op.isCheckBoolean()) {
//							// 有的話 --> 次數變成 1
//							count = 1;
//						}
//						opCountMap.put(op.getOptionsName(), count);
//						// 將結果更新回 codeOpCountMap
//						codeOpCountMap.put(op.getCode(), opCountMap);
//					}
//				}
//				// 至此選項編號和選項已蒐集完畢
//				// 2.3 單選題
//				if (ans.getRadioAnswer() > 0) {
//					// 根據選項編號從 Map<選項編號,Map<選項,次數>> 中取出對應的 Map<選項,次數>
//					Map<String, Integer> opCountMap = codeOpCountMap.get(ans.getRadioAnswer());
//					// 更新次數
//					for (String optionName : opCountMap.keySet()) {
//						// opCountMap 中只會有一筆資料而已，因為一個選項編號下，只會有一個選項和一個次數
//						int count = opCountMap.get(optionName) + 1;
//						opCountMap.put(optionName, count);
//					}
//				}
//				map.put(ans.getQuestionId(), codeOpCountMap);
//			} catch (Exception e) {
//				throw e;
//			}
//		}
//		// 將每一題中每個編號的選項和次數設定回 QuestionCountVo
//		List<QuestionCountVo> voList = setAndGetQuestionCountVoList(map, voMap);
//		Quiz quiz = quizDao.getById(quizId);
//		StatisticVo statisticVo = new StatisticVo(quiz, voList);
//		return new StatisticRes(ResCodeMessage.SUCCESS.getCode(), //
//				ResCodeMessage.SUCCESS.getMessage(), statisticVo);
//	}
//
////	===================================================
//
//	private Map<Integer, QuestionCountVo> setQuestionAnswerVo(int quizId) {
//		// 將問題相關的資訊設定給 QuestionAnswerVo
//		List<Question> questionList = questionDao.getByQuizId(quizId);
//		// 將 Question 轉成 QuestionAnswerVo
//		Map<Integer, QuestionCountVo> map = new HashMap<>();
//		for (Question question : questionList) {
//			// 跳過簡答題
//			if (QuestionType.checkTextType(question.getType())) {
//				continue;
//			}
//			QuestionCountVo vo = new QuestionCountVo(//
//					question.getQuestionId(), question.getName(), //
//					question.getType(), question.isRequired());
//			// 問題編號，vo
//			map.put(question.getQuestionId(), vo);
//		}
//		return map;
//	}
//
//	private List<QuestionCountVo> setAndGetQuestionCountVoList(Map<Integer, Map<Integer, Map<String, Integer>>> map, //
//			Map<Integer, QuestionCountVo> voMap) {
//		List<QuestionCountVo> voList = new ArrayList<>();
//		for (int questionId : map.keySet()) {
//			List<OptionsCount> opCountList = new ArrayList<>();
//			// 取出對應的 Map<選項編號, Map<選項, 次數>>
//			Map<Integer, Map<String, Integer>> codeOpCountMap = map.get(questionId);
//			// 以下2種寫法擇一
//			// 寫法1
//			for (int code : codeOpCountMap.keySet()) {
//				Map<String, Integer> opNameCountMap = codeOpCountMap.get(code);
//				for (String opName : opNameCountMap.keySet()) {
//					int count = opNameCountMap.get(opName);
//					OptionsCount opCount = new OptionsCount(code, opName, count);
//					opCountList.add(opCount);
//				}
//
//			}
//			// 寫法2: 以下是 Lambda 寫法: 執行效率有比上面的程式碼好
//			codeOpCountMap.forEach((code, v) -> {
//				v.forEach((opName, count) -> {
//					OptionsCount opCount = new OptionsCount(code, opName, count);
//					opCountList.add(opCount);
//				});
//			});
//			// voMap 是之前先整理過的 Map<問題編號, QuestionCountVo>，所以所有選擇題都會有
//			QuestionCountVo vo = voMap.get(questionId);
//			vo.setOptionsCountList(opCountList);
//			voList.add(vo);
//		}
//		return voList;
//	}

	public StatisticRes statistic(int quizId) throws Exception {
	    if (quizId <= 0) {
	        return new StatisticRes(ResCodeMessage.QUIZ_ID_ERROR.getCode(),
	                                ResCodeMessage.QUIZ_ID_ERROR.getMessage());
	    }

	    // 將問題相關的資訊設定給 QuestionCountVo（包含文字題）
	    Map<Integer, QuestionCountVo> voMap = setQuestionAnswerVo(quizId);

	    // 使用 QuizId 撈取所有的填答
	    List<Fillin> fillinList = fillinDao.getByQuizId(quizId);

	    // 問題編號 -> 選項編號 -> 選項名稱 -> 次數
	    Map<Integer, Map<Integer, Map<String, Integer>>> map = new HashMap<>();
	    
	    Map<Integer, List<String>> textAnswerMap = new HashMap<>();

	    for (Fillin fillin : fillinList) {
	        try {
	            // 1. 把 answer_str 轉成 Answer
	            Answer ans = mapper.readValue(fillin.getAnswerStr(), Answer.class);

	            // 2. 初始化問題 map（即使文字題也保留）
	            map.putIfAbsent(ans.getQuestionId(), new HashMap<>());
	            Map<Integer, Map<String, Integer>> codeOpCountMap = map.get(ans.getQuestionId());

	            // 2.1 多選題
	            if (ans.getOptionsList() != null) {
	                for (Options op : ans.getOptionsList()) {
	                    Map<String, Integer> opCountMap = codeOpCountMap.getOrDefault(op.getCode(), new HashMap<>());
	                    if (op.isCheckBoolean()) {
	                        int count = opCountMap.getOrDefault(op.getOptionsName(), 0) + 1;
	                        opCountMap.put(op.getOptionsName(), count);
	                    }
	                    codeOpCountMap.put(op.getCode(), opCountMap);
	                }
	            }

	            // 2.2 單選題
	            if (ans.getRadioAnswer() > 0) {
	                Map<String, Integer> opCountMap = codeOpCountMap.getOrDefault(ans.getRadioAnswer(), new HashMap<>());
	                // 只要有 radioAnswer 就 +1
	                if (!opCountMap.containsKey("已選")) {
	                    opCountMap.put("已選", 1); // 可用後端或前端對應選項名稱
	                } else {
	                    opCountMap.put("已選", opCountMap.get("已選") + 1);
	                }
	                codeOpCountMap.put(ans.getRadioAnswer(), opCountMap);
	            }

	            // 2.3 文字題不做次數統計，但 map 已初始化
	            // (前端可以看到 questionId，但 optionsCountList 會是空)
	            map.put(ans.getQuestionId(), codeOpCountMap);
	            Answer ans1 = mapper.readValue(fillin.getAnswerStr(), Answer.class);

	            // 文字題收集答案
	            if (StringUtils.hasText(ans1.getTextAnswer())) {
	                textAnswerMap.putIfAbsent(ans1.getQuestionId(), new ArrayList<>());
	                textAnswerMap.get(ans1.getQuestionId()).add(ans1.getTextAnswer());
	            }

	        } catch (Exception e) {
	            throw e;
	        }
	    }
	    for (QuestionCountVo vo : voMap.values()) {
	        if ("T".equals(vo.getType())) {
	            vo.setTextAnswers(textAnswerMap.getOrDefault(vo.getQuestionId(), new ArrayList<>()));
	        }
	    }

	    // 將每一題中每個編號的選項和次數設定回 QuestionCountVo
	    List<QuestionCountVo> voList = setAndGetQuestionCountVoList(map, voMap);

	    Quiz quiz = quizDao.getById(quizId);
	    StatisticVo statisticVo = new StatisticVo(quiz, voList);

	    return new StatisticRes(ResCodeMessage.SUCCESS.getCode(),
	                             ResCodeMessage.SUCCESS.getMessage(),
	                             statisticVo);
	}


	// 將 Question 的所有選擇題的基本資料(不包括選項和次數)設定到 QuestionCountVo
	private Map<Integer, QuestionCountVo> setQuestionAnswerVo(int quizId) {
	    List<Question> questionList = questionDao.getByQuizId(quizId);
	    Map<Integer, QuestionCountVo> map = new HashMap<>();

	    for (Question question : questionList) {
	        QuestionCountVo vo = new QuestionCountVo(
	                question.getQuestionId(),
	                question.getName(),
	                question.getType(),
	                question.isRequired()
	        );
	        map.put(question.getQuestionId(), vo);
	    }
	    return map;
	}


	private List<QuestionCountVo> setAndGetQuestionCountVoList(Map<Integer, Map<Integer, Map<String, Integer>>> map, //
			Map<Integer, QuestionCountVo> voMap) {
		List<QuestionCountVo> voList = new ArrayList<>();
		for (int questionId : map.keySet()) {
			List<OptionsCount> opCountList = new ArrayList<>();
			// 取出對應的 Map<選項編號, Map<選項, 次數>>
			Map<Integer, Map<String, Integer>> codeOpCountMap = map.get(questionId);
			// 以下2種寫法擇一
			// 寫法1
			for (int code : codeOpCountMap.keySet()) {
				Map<String, Integer> opNameCountMap = codeOpCountMap.get(code);
				for (String opName : opNameCountMap.keySet()) {
					int count = opNameCountMap.getOrDefault(opName, 0);
					OptionsCount opCount = new OptionsCount(code, opName, count);
					opCountList.add(opCount);
				}

			}
			// 寫法2: 以下是 Lambda 寫法: 執行效率有比上面的程式碼好
//			codeOpCountMap.forEach((code, v) -> {
//				v.forEach((opName, count) -> {
//					OptionsCount opCount = new OptionsCount(code, opName, count);
//					opCountList.add(opCount);
//				});
//			});
			/* voMap 是之前先整理過的 Map<問題編號, QuestionCountVo>，所以所有選擇題都會有 */
			QuestionCountVo vo = voMap.get(questionId);
			vo.setOptionsCountList(opCountList);
			voList.add(vo);
		}
		return voList;
	}
}