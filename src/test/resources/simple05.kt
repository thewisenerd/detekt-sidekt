@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class GET

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class POST

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Path(val descriptor: String)

@POST
@Path("/users/{username}")
fun foo2(a: String? = null) {

}

@GET
@Path("/users/{username}")
fun foo3(a: String? = "foo") {

}
