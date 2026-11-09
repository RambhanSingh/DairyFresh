package com.milk.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Shows validation problems as a red message on the page the user came from. */
@ControllerAdvice
public class ErrorAdvice {

    @ExceptionHandler(IllegalArgumentException.class)
    public String badInput(IllegalArgumentException e, HttpServletRequest request, RedirectAttributes ra) {
        return back(e.getMessage(), request, ra);
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public String missingOrInvalid(Exception e, HttpServletRequest request, RedirectAttributes ra) {
        return back("Please fill in all the required fields with valid values.", request, ra);
    }

    private String back(String message, HttpServletRequest request, RedirectAttributes ra) {
        ra.addFlashAttribute("error", message);
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null && !referer.isBlank() ? referer : "/");
    }
}
