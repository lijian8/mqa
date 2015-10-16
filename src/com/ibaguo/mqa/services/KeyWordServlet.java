package com.ibaguo.mqa.services;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.ibaguo.mqa.intefaces.KeyWordExtract;
import com.ibaguo.mqa.pack.impl.NlpKeyWordExtract;
import com.ibaguo.mqa.util.Utils;
import com.ibaguo.nlp.MyNLP;

public class KeyWordServlet extends HttpServlet {
		private static final long serialVersionUID = 1L;

		public KeyWordServlet() {
		}

		protected void doGet(HttpServletRequest request, HttpServletResponse response)
				throws ServletException, IOException {
			String sent = request.getParameter("sent");
			String num = request.getParameter("num");
			Integer rNum = Integer.valueOf(num);
			response.setContentType("application/json;charset=utf-8");
			response.setCharacterEncoding("UTF-8");
			response.setStatus(HttpServletResponse.SC_OK);
			KeyWordExtract kwe = new NlpKeyWordExtract();
			List<String> kwList = kwe.extractKeyword(sent, rNum);
			response.getWriter().println(Utils.toJson(kwList));
			response.getWriter().println("session=" + request.getSession(true).getId());
		}
	}