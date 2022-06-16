package weblib;

import jmxlib.AreaCalc;
import jmxlib.AreaCalcMBean;
import jmxlib.ResChecker;
import jmxlib.ResCheckerMBean;
import lombok.Data;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.persistence.*;
import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

@Data
@ManagedBean
@SessionScoped
public class ResultBean implements Serializable {
    private EntityManagerFactory entityManagerFactory;
    private EntityManager entityManager;
    private EntityTransaction transaction;
    private Result newResult;
    private List<?> results;
    private AreaCalcMBean areaCalc;
    private ResCheckerMBean resChecker;


    public ResultBean() throws Exception {
        newResult = new Result();
        results = new ArrayList<>();

        entityManagerFactory = Persistence.createEntityManagerFactory("PostgresPU");
        entityManager = entityManagerFactory.createEntityManager();
        transaction = entityManager.getTransaction();

        MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        ObjectName resName = new ObjectName("weblib:type=ResCheckerMBean");
        ObjectName areaName = new ObjectName("weblib:type=AreaCalcMBean");
        resChecker = new ResChecker();
        areaCalc = new AreaCalc();
        mBeanServer.registerMBean(resChecker, resName);
        mBeanServer.registerMBean(areaCalc, areaName);
    }

    public void addResult() {
        newResult.generateStatus();
        newResult.generateTime();

        try {
            transaction.begin();
            entityManager.persist(newResult);
            areaCalc.calcArea(newResult.getR_value());
            resChecker.countOutPointsAmount(newResult.checkQuarters());
            newResult = new Result();
            Query query = entityManager.createQuery("SELECT e FROM Result e");
            results = query.getResultList();
            resChecker.countPointsAmount(results.size());
            transaction.commit();
        } catch (RuntimeException e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw e;
        }
    }

    public void clearAll() {
        try {
            transaction.begin();
            Query query = entityManager.createQuery("DELETE FROM Result ");
            query.executeUpdate();
            results.clear();
            transaction.commit();
        } catch (RuntimeException e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw e;
        }
    }
}
