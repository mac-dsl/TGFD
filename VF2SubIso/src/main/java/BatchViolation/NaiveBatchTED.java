package BatchViolation;

import infra.*;

import java.time.LocalDate;
import java.util.*;

public class NaiveBatchTED {

    private MatchCollection matches;
    private TGFD tgfd;


    public NaiveBatchTED(MatchCollection allMatches, TGFD tgfd)
    {
        this.tgfd=tgfd;
        this.matches=allMatches;
    }

    public Set<Violation> findViolations()
    {
        Set<Violation> violations=new HashSet<>();
        Delta delta=tgfd.getDelta();
        LocalDate []allSnapshots= (LocalDate[]) matches.getTimeStamps().toArray();
        for(int i=0;i<allSnapshots.length;i++)
        {
            List<Match> firstMatches=matches.getMatches(allSnapshots[i]);
            for (int j=i+1;j<allSnapshots.length;j++)
            {
                Interval intv=new Interval(allSnapshots[i],allSnapshots[j]);
                if(intv.inDelta(delta.getMin(),delta.getMax()))
                {
                    List<Match> secondMatches=matches.getMatches(allSnapshots[i]);
                    for (Match first:firstMatches) {
                        String firstSignatureX=first.getSignatureX();
                        String firstSignatureY=Match.signatureFromY(tgfd.getPattern(),first.getMapping(),tgfd.getDependency().getY());
                        for (Match second:secondMatches) {
                            if(firstSignatureX.equals(second.getSignatureX()))
                            {
                                //Here, they both should have the same signature Y
                                String secondSignatureY=Match.signatureFromY(tgfd.getPattern(),second.getMapping(),tgfd.getDependency().getY());
                                if(!firstSignatureY.equals(secondSignatureY))
                                {
                                    //Violation happened.
                                    violations.add(new Violation(first,second,intv));
                                }
                            }
                        }
                    }
                }
            }
        }
        return violations;
    }
}
