// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";
import "@openzeppelin/contracts/token/ERC20/extensions/ERC20Burnable.sol";
import "@openzeppelin/contracts/access/AccessControl.sol";

contract RSUToken is ERC20, ERC20Burnable, AccessControl {
    bytes32 public constant CAFETERIA_ROLE = keccak256("CAFETERIA_ROLE");
    bytes32 public constant RSU_ROLE = keccak256("RSU_ROLE");
	address public universidad;
	
    event AttendanceAwarded(address indexed student, uint256 amount);
	event FoodPurchased (address indexed student, address indexed cafeteria, uint256 amount);
	event TokensReturned(address indexed cafeteria, address indexed universidad, uint256 amount);

    constructor() ERC20("RSU Healthy Token", "RSU") {
        _grantRole(DEFAULT_ADMIN_ROLE, msg.sender);
		_grantRole(RSU_ROLE, msg.sender);
		universidad = msg.sender;
    }

    function awardAttendance(address student, uint256 amount)
        external onlyRole(RSU_ROLE)
    {
        _mint(student, amount);
        emit AttendanceAwarded(student, amount);
    }
	
    function purchaseFood(address student, address cafeteria, uint256 amount) external {
		require(student == msg.sender, "Solo el alumno puede pagar");
        require(hasRole(CAFETERIA_ROLE, cafeteria), "RSUToken: destino no es cafeteria autorizada");
        _transfer(student, cafeteria, amount);
        emit FoodPurchased(student, cafeteria, amount);
    }

    function redeem(uint256 amount)
        external onlyRole(CAFETERIA_ROLE)
    {
        _burn(msg.sender, amount);
        _mint(universidad, amount);
        emit TokensReturned(msg.sender, universidad, amount);
    }
}
