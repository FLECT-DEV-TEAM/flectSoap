package jp.co.flect.soap.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * TypedObjectでSOAPのフィールドに対応するフィールド／メソッドに付与するAnnotation
 * 引数はWSDLで定義されたオブジェクトのフィールド名を示す
 */
@Target({ElementType.METHOD, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface SoapField {
	String value() default "";
}
