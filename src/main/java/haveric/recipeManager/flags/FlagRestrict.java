package haveric.recipeManager.flags;

public class FlagRestrict extends Flag {

    @Override
    protected String getFlagType() {
        return FlagType.RESTRICT;
    }

    @Override
    protected String[] getArguments() {
        return new String[] {
            "{flag} [fail message]", };
    }

    @Override
    protected String[] getDescription() {
        return new String[] {
            "Restricts the recipe for everybody.",
            "This is the player-friendly version of @remove because crafter gets a message when trying to craft the recipe.",
            "",
            "Optionally you can overwrite the default restrict message.", };
    }

    @Override
    protected String[] getExamples() {
        return new String[] {
            "{flag}",
            "{flag} <red>Access denied!", };
    }


    private String message;

    public FlagRestrict() {
    }

    public FlagRestrict(FlagRestrict flag) {
        message = flag.message;
    }

    @Override
    public FlagRestrict clone() {
        return new FlagRestrict((FlagRestrict) super.clone());
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String newMessage) {
        message = newMessage;
    }

    @Override
    protected boolean onParse(String value) {
        setMessage(value);
        return true;
    }

    @Override
    protected void onCheck(Args a) {
        a.addReason("flag.restrict", message);
    }
}
