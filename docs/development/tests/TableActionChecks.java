package myfirstmodule;

/** Standalone policy and CSV security checks; does not start Mendix or touch business records. */
public final class TableActionChecks {
    public static void main(String[] args) {
        var global=new TableLifecycleService.Policy(30,false,true,true);
        var club=new TableLifecycleService.Policy(90,false,false,true);
        var federation=new TableLifecycleService.Policy(60,true,true,false);
        var combined=TableLifecycleService.combine(TableLifecycleService.combine(global,club),federation);
        check(combined.days()==90,"The longer club retention must survive a shorter federation override.");
        check(combined.forever(),"An indefinite retention rule must prevent automatic expiry.");
        check(!combined.allowDelete(),"A club deletion prohibition must survive federation permission.");
        check(!combined.autoPurge(),"Automatic deletion needs every applicable policy to allow it.");
        var attemptedWeakening=TableLifecycleService.combine(global,new TableLifecycleService.Policy(1,false,true,true));
        check(attemptedWeakening.days()==30,"An organisation cannot shorten global retention.");
        var globalBlock=TableLifecycleService.combine(new TableLifecycleService.Policy(30,true,false,false),new TableLifecycleService.Policy(365,false,true,true));
        check(globalBlock.forever()&&!globalBlock.allowDelete()&&!globalBlock.autoPurge(),"Organisation overrides cannot weaken global prohibitions.");
        for(String unsafe:new String[]{"=SUM(A1:A2)","+cmd","-2+3","@SUM(A1)"," \t=1+1","\r\n@SUM(A1)"})
            check(TableLifecycleService.csvValue(unsafe).startsWith("\"'"),"Spreadsheet formula injection was not neutralized.");
        check(TableLifecycleService.csvValue("Leuven, \"North\"\nClub").equals("\"Leuven, \"\"North\"\"\nClub\""),"Quotes, commas and line breaks must remain valid CSV data.");
        check(TableLifecycleService.csvValue(null).equals("\"\""),"Empty cells must remain empty.");
        check(TableLifecycleService.csvValue("Équipe fictive").equals("\"Équipe fictive\""),"Unicode names must be preserved.");
        System.out.println("PASS: retention precedence, deletion prohibitions, automatic-purge opt-in and CSV injection/escaping checks.");
    }
    private static void check(boolean valid,String message){if(!valid)throw new AssertionError(message);}
}
