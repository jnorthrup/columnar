package com.gossipmesh.core

class GossiperOptions {
  public  var protocolPeriodMs = 1000
  public  var pingTimeoutMs = 200
  public  var indirectPingTimeoutMs = 400
  public  var deathTimeoutMs = 60000
  public  var fanoutFactor = 3
    public var numberOfIndirectEndPoints = 3
}