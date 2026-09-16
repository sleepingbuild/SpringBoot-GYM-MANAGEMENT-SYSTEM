# Test

Mỗi agent viết test trong package con tương ứng module của mình, vd:
`com.gym.management.service.CheckInServiceTest` (Agent 3),
`com.gym.management.service.PaymentServiceTest` (Agent 4)...

Ưu tiên test tầng Service (business logic). Dùng H2 in-memory (đã có trong pom.xml,
scope test) cho integration test cần DB thật, không cần Postgres chạy local.
