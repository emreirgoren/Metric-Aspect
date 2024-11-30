@Slf4j
@Aspect
@Component
public class MetricAspect {
    public static final String METRIC_KEY = "metric.aspect";
    public static final String PACKAGE_NAME = "com.journey";
    private final MeterRegistry meterRegistry;

    public MetricAspect(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Around("execution(* com.journey.controller..*.*(..)) || execution(* com.journey.service..*.*(..)) ||  execution(* com.journey.repository..*.*(..)) ")
    public Object customTimedControllerAndService(ProceedingJoinPoint joinPoint) throws Throwable {
        return handleMetric(joinPoint);
    }

    private Object handleMetric(ProceedingJoinPoint joinPoint) throws Throwable {
        var metricName = generateMetricName(joinPoint);
        return recordTime(joinPoint, metricName);
    }

    private Object recordTime(ProceedingJoinPoint joinPoint, String metricName) throws Throwable {
        Timer.Sample sample = Timer.start(meterRegistry);
        var result = joinPoint.proceed();
        sample.stop(meterRegistry.timer(metricName));
        return result;
    }

    private boolean applicationPackage(ProceedingJoinPoint joinPoint) {
        return joinPoint.getSignature().toString().contains(PACKAGE_NAME);
    }

    private String generateMetricName(ProceedingJoinPoint joinPoint) {
        if (applicationPackage(joinPoint)) {
            return METRIC_KEY +
                    joinPoint.getSignature().toString().substring(joinPoint.getSignature().toString().indexOf(PACKAGE_NAME));
        } else {
            return METRIC_KEY +
                    Arrays.stream(joinPoint.getTarget().getClass().getInterfaces()).findFirst().get().getName() + "." +
                    joinPoint.getSignature().getName();
        }
    }
}