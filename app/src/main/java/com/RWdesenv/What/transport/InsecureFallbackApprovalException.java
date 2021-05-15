package com.RWdesenv.What.transport;

public class InsecureFallbackApprovalException extends Exception {
  public InsecureFallbackApprovalException(String detailMessage) {
    super(detailMessage);
  }

  public InsecureFallbackApprovalException(Throwable e) {
    super(e);
  }
}
