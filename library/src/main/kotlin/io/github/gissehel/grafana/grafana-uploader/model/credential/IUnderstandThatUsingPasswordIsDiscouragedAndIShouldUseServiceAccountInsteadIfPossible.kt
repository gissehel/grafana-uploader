package io.github.gissehel.grafana.grafanauploader.model.credential

@RequiresOptIn(message = "Using password is insecure. Please, only use password if service account is not an option.")
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class IUnderstandThatUsingPasswordIsDiscouragedAndIShouldUseServiceAccountInsteadIfPossible
