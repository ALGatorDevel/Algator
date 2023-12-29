/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package si.fri.algator.server;

/**
 *
 * @author tomaz
 */
public enum ASCommandStatus {
  UNKNOWN(0), NEW(1), RUNNING(2), DONE(3), STOPPED(4), ERROR(-1);
  
  private int code;

  private ASCommandStatus(int code) {
    this.code = code;
  }  
  public int getCode() { return code; }
}
