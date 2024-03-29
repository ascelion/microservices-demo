package ascelion.micro.basket;

import ascelion.micro.reservation.api.ReservationExpired;

import static ascelion.micro.shared.utils.LogUtils.loggerForThisClass;
import static java.util.Arrays.asList;

import org.slf4j.Logger;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ReservationExpiredListener {
	static private final Logger L = loggerForThisClass();

	@Autowired
	private BasketItemRepo repo;

	@RabbitListener(queues = ReservationExpired.QUEUE_NAME)
	public void run(ReservationExpired[] payload) {
		L.info("Expired: {}", asList(payload));

		for (final ReservationExpired exp : payload) {
			this.repo.findByProductId(exp.getOwnerId(), exp.getProductId())
					.ifPresent(item -> {
						item.expire();

						this.repo.save(item);
					});
		}
	}
}
