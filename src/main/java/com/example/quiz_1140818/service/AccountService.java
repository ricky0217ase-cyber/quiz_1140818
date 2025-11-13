package com.example.quiz_1140818.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.quiz_1140818.constants.ResCodeMessage;
import com.example.quiz_1140818.dao.AccountDao;
import com.example.quiz_1140818.entity.Account;
import com.example.quiz_1140818.response.BasicRes;

@Service
public class AccountService {

	private BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
	
	@Autowired
	private AccountDao accountDao;
	
	public BasicRes addInfo(String account , String password) {
		try {
			// 若文件有說明在新增資訊之前要先檢查帳號是否存在
			int count = accountDao.selectCountByAccount(account);
			//因為是透過 PK 欄位 account 來查詢是否有存在值，所以 count 只會有 0 或 1
			if(count == 1) {
				return new BasicRes(ResCodeMessage.ACCOUNT_EXIST.getCode(),//
						ResCodeMessage.ACCOUNT_EXIST.getMessage());
			}
			//存進 DB 中的密碼要記得加密
			accountDao.addInfo(account, encoder.encode(password));
			return new BasicRes(ResCodeMessage.SUCCESS.getCode(),//
					ResCodeMessage.SUCCESS.getMessage());
		} catch (Exception e) {
			// 若是 PK 已存在，新增資料就會失敗
			// 發生 Exception 時，可以有以下2種處理方式:
			// a. 固定的回覆訊息，但真正的錯誤原因無法顯示
//			return new BasicRes(ResCodeMessage.ADD_INFO_FAIRED.getCode(),//
//					ResCodeMessage.ADD_INFO_FAIRED.getMessage());
			
			// b. 將 catch 到的例外(Exception)拋出(throw)，再由自定義的類別
			//    GlobalExceptionHandler 寫入(回覆)真正的錯誤訊息
			throw e;
		}
	}
	
	public BasicRes login(String account , String password) {
		// 使用 account 取得對應資料
		Account data = accountDao.selectByAccount(account);
		if(data == null) { // data == null 表示沒資料 --> 也表示該帳號不存在
			return new BasicRes(ResCodeMessage.NOT_FOUND.getCode(),//
					ResCodeMessage.NOT_FOUND.getMessage());
		}
		
		// 比對密碼 :　使用排除法，所以前面記得要有　！　，表示密碼匹配不成功
		if(!encoder.matches(password, data.getPassword())) {
			return new BasicRes(ResCodeMessage.PASSWORD_MISMATCH.getCode(),//
					ResCodeMessage.PASSWORD_MISMATCH.getMessage());
		}
		
		return new BasicRes(ResCodeMessage.SUCCESS.getCode(),//
				ResCodeMessage.SUCCESS.getMessage());
	}
}
