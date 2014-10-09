package main.java.ch.ethz.systems.asl.bean;

public class ServiceResponse {
    private boolean isSuccessful;
    private String errorMessage;

    public boolean isSuccessful()
    {
        return isSuccessful;
    }

    public void setSuccessful(boolean isSuccessful)
    {
        this.isSuccessful = isSuccessful;
    }

    public String getErrorMessage()
    {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage)
    {
        this.errorMessage = errorMessage;
    }
}
