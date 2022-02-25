package com.example.testjunit;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.aggregator.AggregateWith;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;
import org.junit.jupiter.params.aggregator.ArgumentsAggregationException;
import org.junit.jupiter.params.aggregator.ArgumentsAggregator;
import org.junit.jupiter.params.converter.ArgumentConversionException;
import org.junit.jupiter.params.converter.ConvertWith;
import org.junit.jupiter.params.converter.SimpleArgumentConverter;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

//1. 확장모델 등록 방법1: @ExtendWith 사용하기 -> 기본 생성자 이용
// THRESHOLD 값을 넣어 줄 수 있는 방법이 없다.
//@ExtendWith(FindSlowTestExtension.class)
class StudyTest {

    int value = 1;

    //2. 확장모델 등록 방법2: THRESHOLD 값을 변경할 수 있다.
    // FindSlowExtension.class에 threshold가 생성자로 들어가야함.
    // 따라서 THRESHOLD 값을 넣어 줄 수 있다.
    @RegisterExtension
    static FindSlowTestExtension findSlowTestExtension
            = new FindSlowTestExtension(1000L);



    @FastTest
    void create() {
        Study study = new Study(1);
        assertNotNull(study);
        assertEquals(StudyStatus.DRAFT, study.getStatus(),
                () -> "스터디를 처음 만들면" + StudyStatus.DRAFT + " 상태이다.");
    }

    @DisplayName("예외 발생 테스트 1")
    @FastTest
    void create_throw_one() {
        assertThrows(IllegalArgumentException.class, () -> new Study(-10));
    }


    @DisplayName("예외 발생 테스트2,예외메시지 파라미터로 받기")
    @FastTest
    void create_throw_two() {
        IllegalArgumentException exception
                = assertThrows(IllegalArgumentException.class, () -> new Study(-10));
        String message = exception.getMessage();
        assertEquals("limit은 0보다 커야 합니다.", message);
    }


    @DisplayName("timeout")
    @FastTest
    void create_timeout() {
        assertTimeout(Duration.ofMillis(400), () -> {
            new Study(10);
            Thread.sleep(300);
        });
    }


    @DisplayName("timeoutPreemptively_ 100milis 넘으면 즉각 종료")
    @SlowTest
    void create_timeout_preemptively() {
        assertTimeoutPreemptively(Duration.ofMillis(400), () -> {
            new Study(10);
            Thread.sleep(300);
        });

    }

    @DisplayName("조건에 따른 테스트1")
    @SlowTest
    void create_assume_one() {
        assumeTrue("skaru".equalsIgnoreCase(System.getenv("USERNAME")));

        // 위의 조건을 통과해야 다음의 테스트가 실행
        Study actual = new Study(10);
        assertThat(actual.getLimit()).isGreaterThan(0);
    }

    @DisplayName("조건에 따른 테스트2")
    @SlowTest
    void create_assumingThat() {

        assumingThat("skaru".equalsIgnoreCase(System.getenv("USERNAME")), () -> {
            System.out.println("skaru");
            // 위의 조건을 통과해야 다음의 테스트가 실행
            Study actual = new Study(10);
            assertThat(actual.getLimit()).isGreaterThan(0);
        });

        assumingThat("Windows_NT".equalsIgnoreCase(System.getenv("OS")), () -> {
            System.out.println("Window");
            // 위의 조건을 통과해야 다음의 테스트가 실행
            Study actual = new Study(10);
            assertThat(actual.getLimit()).isGreaterThan(0);
        });

    }

    @DisplayName("repeat테스트")
    @RepeatedTest(value = 10, name = "{displayName}, {currentRepetition}/{totalRepetitions}" )
    void repeatTest(RepetitionInfo repetitionInfo) {
        System.out.println("test" + repetitionInfo.getCurrentRepetition() + "/" + repetitionInfo.getTotalRepetitions() );
    }

    @DisplayName("parameterized테스트")
    @ParameterizedTest(name = "{index} {displayName} message={0}")
    @ValueSource(strings = {"날씨가", "많이", "추워지고", "있네요."})
    void parameterizedTest(String message) {
        System.out.println(message);
    }

    @DisplayName("인자값의 type 변환")
    @ParameterizedTest(name = "{index} {displayName} message={0}")
    @ValueSource(strings = {"날씨가", "많이", "추워지고", "있네요."})
    @NullAndEmptySource
    void parameterizedTest2(String message) {
        System.out.println(message);
    }

    @DisplayName("인자값의 type 변환")
    @ParameterizedTest(name = "{index} {displayName} message={0}")
    @ValueSource(ints = {10, 20, 30, 40})
    void parameterizedTest3(@ConvertWith(StudyConverter.class) Study study) {
        System.out.println(study.getLimit());
    }

    static class StudyConverter extends SimpleArgumentConverter {
        @Override
        protected Object convert(Object source, Class<?> targetType) throws ArgumentConversionException {
            assertEquals(Study.class, targetType, "Can only convert to Study");
            return new Study(Integer.parseInt(source.toString()));
        }
    }

    /** CsvSource를 사용할 때 ""내부의 값이 하나의 인자로,
     * 인자값의 타입변환을 위해 구현된 custom converter인 StudyConver는 하나의 인자(argument)만 적용되기에
     * CsvSource에서의 여러가지 인자가 존재하는 경우는 적용할 수 없다.
     * 여러 argument 존재 시,
     * 방법 1) 테스트 안에서 argument를 각각 하나씩 받아서 만들 수 있다. **/
    @DisplayName("CsvSource 이용해 2개인자넘기기")
    @ParameterizedTest(name = "{index} {displayName} message={0}")
    @CsvSource({"10, '자바 스터디'", "20, 스프링"})
    void parameterizedTest4(Integer limit, String name) {
        Study study = new Study(limit, name);
        System.out.println(study);
    }

    /**
     * 여러 argument 존재 시,
     * 방법 2) Study로 받은 후, 2개의 argument를 하나로 aggregation하기.
     *        인자값을 조합하는 ArgumentAccessor이용 **/
    @DisplayName("ArgumentAccessor이용")
    @ParameterizedTest(name = "{index} {displayName} message={1}")
    @CsvSource({"10, '자바 스터디'", "20, 스프링"})
    void parameterizedTest5(ArgumentsAccessor argumentsAccessor) {
        Study study = new Study(argumentsAccessor.getInteger(0), argumentsAccessor.getString(1));
        System.out.println(study);
    }

    /**
     * 여러 argument 존재 시,
     * 방법 2) Study로 받은 후, 2개의 argument를 하나로 aggregation하기.
     *        aggregator 사용해 custom한 argument 만들기 **/
    @DisplayName("custom한 argument")
    @ParameterizedTest(name = "{index} {displayName} message={1}")
    @CsvSource({"10, '자바 스터디'", "20, 스프링"})
    void parameterizedTest6(@AggregateWith(StudyAggregator.class) Study study) {
        System.out.println(study);
    }
    /** aggregator의 제약조건으로 반드시 static한 inner Class이거나 public class 이어야한다. **/
    static class StudyAggregator implements ArgumentsAggregator {
        @Override
        public Object aggregateArguments(ArgumentsAccessor accessor, ParameterContext context) throws ArgumentsAggregationException {
            return new Study(accessor.getInteger(0), accessor.getString(1));
        }
    }

    @DisplayName("공용사용테스트1")
    @SlowTest
    void shareTestInstance() {
        System.out.println(this);
        System.out.println(value++);
        Study study = new Study(1);
        assertThat(study.getLimit()).isGreaterThan(0);
        //여기서 value값이 1
    }

    @DisplayName("공용사용테스트2")
    @FastTest
    void shareTestInstance2() {
        System.out.println(this);
        System.out.println("create => \t" + value++);
        //여기서 value값이 2

    }

    @DisplayName("extension테스트")
    @SlowTest
    void extensionTest() throws InterruptedException {
        Thread.sleep(1005L);
        System.out.println(this);
        System.out.println("create => \t" + value++);

    }
}