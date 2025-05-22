package com.parser.config;

import java.util.Map;

public class Rule {
    private String detector;
    private String metric;
    private String operator;
    private String type;
    private Object value;
    private Integer minCount;
    private Map<String, Object> params;
    private String containsNode;
    
    public String getDetector() { return detector; }
    public void setDetector(String detector) { this.detector = detector; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public String getMetric() { return metric; }
    public void setMetric(String metric) { this.metric = metric; }
    
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }
    
    public Object getValue() { return value; }
    public void setValue(Object value) { this.value = value; }
    
    public Integer getMinCount() { return minCount; }
    public void setMinCount(Integer minCount) { this.minCount = minCount; }
    
    public Map<String, Object> getParams() { return params; }
    public void setParams(Map<String, Object> params) { this.params = params; }
    
    public String getContainsNode() { return containsNode; }
    public void setContainsNode(String containsNode) { this.containsNode = containsNode; }
}
