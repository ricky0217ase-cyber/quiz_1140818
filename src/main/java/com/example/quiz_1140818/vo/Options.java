package com.example.quiz_1140818.vo;

import com.example.quiz_1140818.constants.ConstantsMessage;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class Options {

	@Min(value = 1, message = ConstantsMessage.QUESTION_OPTIONS_CODE_ERROR)
	private int code;

	@NotBlank(message = ConstantsMessage.QUESTION_OPTIONS_NAME_ERROR)
	private String optionsName;

	private boolean checkBoolean;

	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}

	public String getOptionsName() {
		return optionsName;
	}

	public void setOptionsName(String optionsName) {
		this.optionsName = optionsName;
	}

	public boolean isCheckBoolean() {
		return checkBoolean;
	}

	public void setCheckBoolean(boolean checkBoolean) {
		this.checkBoolean = checkBoolean;
	}

}
