package ascelion.micro.reservations;

import java.math.BigDecimal;
import java.util.UUID;

import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReservationRequest {
	@NotNull
	private UUID productId;
	@NotNull
	private UUID ownerId;
	@NotNull
	private BigDecimal quantity;
}